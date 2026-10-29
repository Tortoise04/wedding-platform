package com.wedding.service.impl;

import com.wedding.dto.EvaluationAuditRequest;
import com.wedding.dto.EvaluationReplyRequest;
import com.wedding.dto.EvaluationRequest;
import com.wedding.entity.Evaluation;
import com.wedding.entity.Merchant;
import com.wedding.entity.WeddingOrder;
import com.wedding.enums.AuditStatus;
import com.wedding.enums.OrderStatus;
import com.wedding.exception.BusinessException;
import com.wedding.repository.EvaluationRepository;
import com.wedding.repository.WeddingOrderRepository;
import com.wedding.repository.WeddingServiceRepository;
import com.wedding.service.EvaluationService;
import com.wedding.service.MerchantService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 厨师:评价这道菜(写库前过四道前置闸门)。
//
// 前三道是老规矩:只能评自己的订单 / 订单须 COMPLETED / 一单一评(uk_order_id 唯一键兜底)。
// 本轮加了第四道 —— 【公开闸门】:落库即 PENDING,管理员放行后才对外可见。
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final WeddingOrderRepository weddingOrderRepository;
    private final WeddingServiceRepository weddingServiceRepository;
    private final MerchantService merchantService;

    // 创建评价:依次过归属 / 状态 / 幂等 / 评分 1~5 四道闸门,再落库。
    // ★ 落库时 auditStatus 一律 PENDING —— 用户提交只是"申请公开",放行权在管理员手里。
    // 同一订单的评价串行化:订单行锁负责跨事务,进程内锁只兜同实例连点;
    // 唯一索引 uk_order_id 是最后一道闸,撞上时由 catch 翻译成中文提示(测试记录 D-37)。
    private static final java.util.concurrent.ConcurrentHashMap<String, Object> EVAL_LOCKS = new java.util.concurrent.ConcurrentHashMap<>();

    @Transactional
    @Override
    public Evaluation createEvaluation(Long userId, EvaluationRequest request) {
        if (request.getOrderId() == null) {
            throw new BusinessException("请选择要评价的订单");
        }
        if (request.getContent() != null && request.getContent().length() > 1000) {
            throw new BusinessException("评价内容不能超过 1000 字");
        }
        // 图片:逗号分隔,去掉空项后最多 9 张;拼起来也不能撑破 images 列的 1000 字符。
        // 这两道放在查订单之前 —— 明显不合规的请求没必要先去锁订单行。
        List<String> imageList = new ArrayList<>();
        if (request.getImages() != null) {
            for (String one : request.getImages().split(",")) {
                if (!one.isBlank()) {
                    imageList.add(one.trim());
                }
            }
        }
        if (imageList.size() > 9) {
            throw new BusinessException("评价图片最多 9 张");
        }
        String images = imageList.isEmpty() ? null : String.join(",", imageList);
        if (images != null && images.length() > 1000) {
            throw new BusinessException("评价图片过多，请减少后再提交");
        }
        // 视频:最多 1 个,地址列宽 255;空串当没传
        String videoUrl = request.getVideoUrl();
        if (videoUrl != null && videoUrl.isBlank()) {
            videoUrl = null;
        }
        if (videoUrl != null && videoUrl.length() > 255) {
            throw new BusinessException("视频地址过长，请重新上传");
        }
        // 匿名:没传就是实名展示
        boolean anonymous = Boolean.TRUE.equals(request.getAnonymous());
        WeddingOrder order = weddingOrderRepository.findByIdForUpdate(request.getOrderId())
                .orElseThrow(() -> new BusinessException("订单不存在: " + request.getOrderId()));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权评价该订单");               // 归属校验
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException("订单完成后才能评价");            // 状态校验
        }
        Integer score = request.getScore();
        if (score == null || score < 1 || score > 5) {
            throw new BusinessException("评分必须在 1~5 之间");
        }

        synchronized (EVAL_LOCKS.computeIfAbsent("eval:" + request.getOrderId(), k -> new Object())) {
            if (evaluationRepository.existsByOrderId(request.getOrderId())) {
                throw new BusinessException("该订单已评价过");                // 幂等
            }

            Evaluation evaluation = new Evaluation();
            evaluation.setOrderId(order.getId());
            evaluation.setUserId(userId);
            evaluation.setMerchantId(order.getMerchantId());
            evaluation.setServiceId(order.getServiceId());
            evaluation.setScore(score);
            evaluation.setContent(request.getContent());
            evaluation.setImages(images);
            evaluation.setVideoUrl(videoUrl);
            evaluation.setAnonymous(anonymous);
            evaluation.setAuditStatus(AuditStatus.PENDING);   // ★ 新评价一律先进"待审核",不直接对外可见
            try {
                return evaluationRepository.saveAndFlush(evaluation);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // 兜底:t_evaluation.order_id 有唯一索引,
                // 万一并发越过前置检查,唯一索引会拦下来 —— 也要给中文提示,不能漏成 500。
                throw new BusinessException("该订单已评价过");
            }
        }
    }

    // 【用户端】我写过的评价 —— 只按 userId 过滤,不看 auditStatus:
    // 用户要能看到自己待审、已通过、被驳回的评价,前端的「评价 / 已评价」按钮态就靠这份数据。
    @Override
    public List<Evaluation> listMyEvaluations(Long userId) {
        return evaluationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // 【管理端】评价列表 —— auditStatus 传 null 就是"待审 + 通过 + 驳回"全要,按评价时间倒序。
    // 和公告 / 资讯的管理端列表同一套路:动态拼条件,并补一个 id 降序当"定序器"
    // (created_at 是秒级精度,同一秒的多条记录只按它排序顺序不稳定)。
    @Override
    public List<Evaluation> listForAdmin(AuditStatus auditStatus) {
        Specification<Evaluation> spec = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (auditStatus != null) {
                conditions.add(cb.equal(root.get("auditStatus"), auditStatus));
            }
            if (query != null) {
                query.orderBy(cb.desc(root.get("createdAt")), cb.desc(root.get("id")));
            }
            return cb.and(conditions.toArray(new Predicate[0]));
        };
        return evaluationRepository.findAll(spec);
    }

    // 审核一条评价:通过则 APPROVED(公开展示),驳回则 REJECTED 且原因必填。
    //
    // 校验前置:approved 缺失、驳回却无原因,都在写库之前拦下。
    // 状态机规矩:只能审 PENDING 的,已审过的一律拒绝(否则能把通过的评价再驳回一次)。
    @Transactional
    @Override
    public Evaluation auditEvaluation(Long evaluationId, EvaluationAuditRequest request) {
        if (request.getApproved() == null) {
            throw new BusinessException("审核结论不能为空");
        }
        if (!request.getApproved() && (request.getRejectReason() == null || request.getRejectReason().isBlank())) {
            throw new BusinessException("驳回原因不能为空");
        }
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new BusinessException("评价不存在: " + evaluationId));
        if (evaluation.getAuditStatus() != AuditStatus.PENDING) {
            throw new BusinessException("该评价已审核过");
        }
        if (request.getApproved()) {
            evaluation.setAuditStatus(AuditStatus.APPROVED);
        } else {
            evaluation.setAuditStatus(AuditStatus.REJECTED);
            evaluation.setRejectReason(request.getRejectReason());
        }
        evaluation.setAuditTime(LocalDateTime.now());   // 业务时间显式赋值,不用 @UpdateTimestamp 代劳
        Evaluation saved = evaluationRepository.save(evaluation);
        // 审核结论变了,服务评分就得跟着变:通过会把分数拉上去,驳回会把分数拉下来,
        // 两种结论都要重算,所以放在 save 之后统一走这里。
        refreshServiceScore(evaluation.getServiceId());
        return saved;
    }

    // 按该服务【已通过】评价的平均分回写 t_service.score(保留 2 位小数,四舍五入)。
    // 一条 APPROVED 都没有时写 0.00;服务查不到只跳过 —— 评分回写失败不该让整次审核失败。
    private void refreshServiceScore(Long serviceId) {
        Double avg = evaluationRepository.avgScoreByServiceIdAndAuditStatus(serviceId, AuditStatus.APPROVED);
        BigDecimal score = avg == null
                ? new BigDecimal("0.00")
                : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
        weddingServiceRepository.findById(serviceId).ifPresent(service -> {
            service.setScore(score);
            weddingServiceRepository.save(service);
        });
    }

    // 【商家端】本店铺评价:先解析"我是哪个商家",只给已过审的(对外展示视角)。
    @Override
    public List<Evaluation> listMerchantEvaluations(Long userId) {
        Merchant shop = merchantService.getMyShop(userId);
        return evaluationRepository.findByMerchantIdAndAuditStatusOrderByCreatedAtDesc(shop.getId(), AuditStatus.APPROVED);
    }

    // 【商家端】回复评价:只能回本店的、已过审的评价,回复内容必填。
    @Transactional
    @Override
    public Evaluation replyEvaluation(Long userId, Long evaluationId, EvaluationReplyRequest request) {
        Merchant shop = merchantService.getMyShop(userId);
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new BusinessException("评价不存在: " + evaluationId));
        if (!evaluation.getMerchantId().equals(shop.getId())) {
            throw new BusinessException("无权回复其他店铺的评价");
        }
        if (evaluation.getAuditStatus() != AuditStatus.APPROVED) {
            throw new BusinessException("该评价未通过审核,不能回复");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BusinessException("回复内容不能为空");
        }
        evaluation.setReplyContent(request.getContent());
        evaluation.setReplyTime(LocalDateTime.now());
        return evaluationRepository.save(evaluation);
    }

    // 【用户端】当页订单里哪些已经评价过 —— 只看当页 id,一条 in 查询查完,不按订单逐条问。
    // 返回 {"订单id": true}:序列化成 JSON 后键本来就是字符串,前端直接 evaluated[orderId] 取。
    @Override
    public Map<String, Boolean> evaluatedOrderIds(Collection<Long> orderIds) {
        Map<String, Boolean> evaluated = new LinkedHashMap<>();
        if (orderIds == null || orderIds.isEmpty()) {
            return evaluated;   // 当页没订单就别发查询
        }
        for (Evaluation evaluation : evaluationRepository.findByOrderIdIn(orderIds)) {
            evaluated.put(String.valueOf(evaluation.getOrderId()), true);
        }
        return evaluated;
    }
}
