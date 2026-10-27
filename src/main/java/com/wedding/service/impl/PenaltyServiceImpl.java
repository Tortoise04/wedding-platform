package com.wedding.service.impl;

import com.wedding.dto.CreatePenaltyRequest;
import com.wedding.entity.Merchant;
import com.wedding.entity.Penalty;
import com.wedding.entity.WeddingService;
import com.wedding.enums.PenaltyType;
import com.wedding.exception.BusinessException;
import com.wedding.repository.MerchantRepository;
import com.wedding.repository.PenaltyRepository;
import com.wedding.repository.WeddingServiceRepository;
import com.wedding.service.PenaltyService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.wedding.enums.PenaltyType.SERVICE_OFF;

// 厨师:管理端违规处罚这道菜(FR-A-01-4)。
//
// 一次处罚可能动多张表:罚单一张,联动改状态还要碰 t_service 或 t_merchant,
// 故整个方法必须 @Transactional —— 罚单与联动改的状态要么一起成功、要么一起回滚。
@Service
@RequiredArgsConstructor
public class PenaltyServiceImpl implements PenaltyService {

    private final PenaltyRepository penaltyRepository;
    private final MerchantRepository merchantRepository;
    private final WeddingServiceRepository weddingServiceRepository;

    // 全站处罚记录(FR-A-01-4)—— 按 商家 / 处罚类型 / 生效状态 任意组合筛选。
    // 和订单监管一样用 Specification "运行时按需往 where 后面加条件"。
    // 只读查询,不加 @Transactional(没有写库动作)。
    @Override
    public List<Penalty> listAllPenalties(Long merchantId, PenaltyType penaltyType, Integer status) {
        Specification<Penalty> spec = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (merchantId != null) {
                conditions.add(cb.equal(root.get("merchantId"), merchantId));
            }
            if (penaltyType != null) {
                conditions.add(cb.equal(root.get("penaltyType"), penaltyType));
            }
            if (status != null) {
                conditions.add(cb.equal(root.get("status"), status));
            }
            // 无论筛不筛,一律按处罚时间倒序。
            // ⚠️ created_at 是秒级精度,同秒记录只按它排序不稳定 → 再补 id 降序当 tie-breaker
            query.orderBy(cb.desc(root.get("createdAt")), cb.desc(root.get("id")));
            return cb.and(conditions.toArray(new Predicate[0]));
        };
        List<Penalty> list = penaltyRepository.findAll(spec);
        fillMerchantNames(list);
        return list;
    }

    // 某商家的处罚历史:先确认商家存在,再按时间倒序取出
    @Override
    public List<Penalty> listByMerchant(Long merchantId) {
        if (!merchantRepository.existsById(merchantId)) {
            throw new BusinessException("商家不存在: " + merchantId);
        }
        List<Penalty> list = penaltyRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
        fillMerchantNames(list);
        return list;
    }

    // 私有工具:给一批罚单回填被罚商家名(批量查一次,避免逐条 N+1)。
    // ★ 被封禁/未过审的商家也在 t_merchant 里,能查到名;已删除的商家不在 → merchantName 为 null,前端显示占位。
    private void fillMerchantNames(List<Penalty> penalties) {
        if (penalties.isEmpty()) {
            return;
        }
        List<Long> ids = penalties.stream().map(Penalty::getMerchantId).distinct().toList();
        Map<Long, String> nameMap = merchantRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Merchant::getId, Merchant::getShopName));
        for (Penalty p : penalties) {
            p.setMerchantName(nameMap.get(p.getMerchantId()));
        }
    }

    // 执行处罚(FR-A-01-4):一个动作可能写多张表,本方法全程 @Transactional。
    //
    // 三种类型动作不同:WARNING 只写罚单;SERVICE_OFF 写罚单 + 下架服务(shelf_status = 0);
    // BAN 写罚单 + 停用店铺(t_merchant.status = 0)。后两种要同改两张表,
    // 不能出现"服务下架了、却查不到这张罚单"的半截状态。
    // 四步:① 取参校验 → ② 查商家 → ③ 按类型联动 → ④ 开罚单。
    @Transactional
    @Override
    public Penalty executePenalty(Long adminId, CreatePenaltyRequest request) {

        // ① 取参 + 前置校验(★校验必须放在任何写库动作之前)
        //    原因用 isBlank 判空(纯空格也算空,见缺陷 D-22)
        if (request.getMerchantId()==null){
            throw new BusinessException("被处罚商家不能为空");
        }
        if (request.getPenaltyType()==null){
            throw new BusinessException("处罚类型不能为空");
        }
        if (request.getReason()==null || request.getReason().isBlank()){
            throw new BusinessException("处罚原因不能为空");
        }
        // ② 查被罚商家是否存在(★被罚对象就是 request.getMerchantId(),不要从别处错取)
        Merchant shop = merchantRepository.findById(request.getMerchantId())
                .orElseThrow(() -> new BusinessException("商家不存在: " + request.getMerchantId()));
        // ③ 按类型分流联动(箭头语法不穿透;default 守卫拒绝未知枚举):
        //    WARNING 不联动;SERVICE_OFF 下架服务(★★ 归属校验:svc.getMerchantId()
        //    必须等于 shop.getId(),防"罚 A 店却下架 B 店的服务");BAN 停用店铺
        switch (request.getPenaltyType()){
            case WARNING ->{}
            case SERVICE_OFF ->{
                if (request.getTargetId()==null){
                    throw new BusinessException("服务下架要指定对应下架的服务");
                }
                WeddingService svc = weddingServiceRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new BusinessException("服务不存在: " + request.getTargetId()));
                if (!Objects.equals(svc.getMerchantId(), request.getMerchantId())){
                    throw new BusinessException("处罚对象不匹配");
                }
                svc.setShelfStatus(0);
                weddingServiceRepository.save(svc);
            }
            case BAN -> {
                shop.setStatus(0); merchantRepository.save(shop);
            }
            default -> {
                throw new BusinessException("暂不支持的处罚类型");
            }
        }
        // ④ 开罚单:创建一条处罚记录(new + set + save)
              Penalty p = new Penalty();
              p.setMerchantId(shop.getId());
              // ★ 执行人来自 token,请求体里没有
              p.setOperatorId(adminId);
              p.setPenaltyType(request.getPenaltyType());
              // targetId 仅 SERVICE_OFF 有值,其余类型为 null
              p.setTargetId(request.getTargetId());
              p.setReason(request.getReason());
              p.setStartTime(LocalDateTime.now());
              p.setEndTime(request.getEndTime());     // 空 = 长期有效
              p.setStatus(1);                         // 1 生效中
        return  penaltyRepository.save(p);
    }
}
