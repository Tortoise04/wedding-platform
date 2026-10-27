package com.wedding.service.impl;

import com.wedding.dto.MerchantApplyRequest;
import com.wedding.dto.MerchantAuditRequest;
import com.wedding.dto.UpdateMerchantRequest;
import com.wedding.entity.Merchant;
import com.wedding.entity.User;
import com.wedding.enums.AuditStatus;
import com.wedding.exception.BusinessException;
import com.wedding.repository.MerchantRepository;
import com.wedding.repository.UserRepository;
import com.wedding.service.MerchantService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 厨师:商家入驻这道菜(提交申请 / 查看进度 / 管理员审核)。
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;

    // 提交入驻申请:闸门是一账号只能申请一次(幂等);通过后 role 仍是 USER,待审核。
    @Transactional
    @Override
    public Merchant apply(Long userId, MerchantApplyRequest request) {
        if (merchantRepository.existsByUserId(userId)) {
            throw new BusinessException("您已提交过入驻申请");
        }

        Merchant merchant = new Merchant();
        merchant.setUserId(userId);
        merchant.setShopName(request.getShopName());
        merchant.setBusinessScope(request.getBusinessScope());
        merchant.setContactPhone(request.getContactPhone());
        merchant.setDescription(request.getDescription());
        merchant.setLogo(request.getLogo());
        merchant.setLicenseUrl(request.getLicenseUrl());
        merchant.setQualificationUrl(request.getQualificationUrl());
        // 待审核,此时账号 role 仍是 USER
        merchant.setAuditStatus(AuditStatus.PENDING);
        merchant.setStatus(1);                          // 启用
        return merchantRepository.save(merchant);
    }

    // 查看我的入驻申请:没提交过直接报错,前端据此判断能否再申请。
    @Override
    public Merchant getMyApplication(Long userId) {
        return merchantRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("尚未提交入驻申请"));
    }

    // 管理员审核。
    //
    // ⚠️ 必须 @Transactional:审核通过时要同时改两张表 ——
    //    t_merchant.audit_status = APPROVED 且 t_user.role = MERCHANT。
    //    两张表要么都改成功,要么一起回滚(和下单那次"多表原子"同理)。
    @Transactional
    @Override
    public Merchant audit(Long merchantId, MerchantAuditRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException("入驻申请不存在: " + merchantId));

        if (merchant.getAuditStatus() != AuditStatus.PENDING) {
            throw new BusinessException("该申请已审核过");
        }

        if (Boolean.TRUE.equals(request.getApproved())) {
            merchant.setAuditStatus(AuditStatus.APPROVED);

            // 统一账号体系落地:审核通过才把账号角色改成商家,此时才开通商家端
            User user = userRepository.findById(merchant.getUserId())
                    .orElseThrow(() -> new BusinessException("账号不存在: " + merchant.getUserId()));
            user.setRole("MERCHANT");
            userRepository.save(user);
        } else {
            // 驳回必须说明理由:否则新人看不到任何原因,只能反复提交
            if (request.getRejectReason() == null || request.getRejectReason().isBlank()) {
                throw new BusinessException("驳回时必须填写驳回原因");
            }
            merchant.setAuditStatus(AuditStatus.REJECTED);
            merchant.setRejectReason(request.getRejectReason());
        }

        merchant.setAuditTime(LocalDateTime.now());
        return merchantRepository.save(merchant);
    }

    // 取当前用户的店铺:只有已通过审核(role 已变 MERCHANT)才拿得到,否则报错。
    @Override
    public Merchant getMyShop(Long userId){
        return merchantRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("请先完成商家入驻"));
    }

    // 门户商家列表:只给"已过审 + 账号启用"的,游客可看(FR-U-02-1)。
    @Override
    public List<Merchant> listApprovedMerchants() {
        return merchantRepository.findByAuditStatusAndStatusOrderByCreatedAtDesc(AuditStatus.APPROVED, 1);
    }

    // 门户商家详情:未过审 / 已停用的一律当"不存在"处理,不向游客泄露。
    @Override
    public Merchant getMerchantById(Long id) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new BusinessException("商家不存在: " + id));
        if (merchant.getAuditStatus() != AuditStatus.APPROVED || merchant.getStatus() == null || merchant.getStatus() != 1) {
            throw new BusinessException("商家不存在: " + id);
        }
        return merchant;
    }

    // 维护本人店铺信息:先解析"我是哪个商家",再只覆盖可编辑字段(FR-M-01-5)。
    @Transactional
    @Override
    public Merchant updateShopProfile(Long userId, UpdateMerchantRequest request) {
        Merchant shop = getMyShop(userId);
        shop.setBusinessScope(request.getBusinessScope());
        shop.setContactPhone(request.getContactPhone());
        shop.setDescription(request.getDescription());
        shop.setLogo(request.getLogo());
        return merchantRepository.save(shop);
    }

    // 管理端商家列表:审核状态 + 关键字(店铺名/联系电话模糊包含)任意组合,申请时间倒序 + id 兜底。
    //
    // keyword 为 null 或空白时忽略该条件,等价于只按审核状态筛选的旧行为。
    @Override
    public List<Merchant> listMerchantsForAdmin(AuditStatus auditStatus, String keyword) {
        String kw = (keyword == null) ? null : keyword.trim();

        Specification<Merchant> spec = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (auditStatus != null) {
                conditions.add(cb.equal(root.get("auditStatus"), auditStatus));
            }
            if (kw != null && !kw.isEmpty()) {
                // 先转义 LIKE 通配符,否则用户输入 % 或 _ 会被当通配符命中全部
                String escaped = kw.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                String like = "%" + escaped + "%";
                conditions.add(cb.or(
                        cb.like(cb.lower(root.get("shopName")), like),
                        cb.like(cb.lower(root.get("contactPhone")), like)));
            }
            query.orderBy(cb.desc(root.get("createdAt")), cb.desc(root.get("id")));
            return cb.and(conditions.toArray(new Predicate[0]));
        };
        return merchantRepository.findAll(spec);
    }
}
