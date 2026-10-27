package com.wedding.repository;

import com.wedding.entity.Evaluation;
import com.wedding.enums.AuditStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// 冷库:评价的存取。
//
// ★ 本轮改造的重点就在下面两行方法名上:
//   加审核之前,它们是 findByServiceIdOrderByCreatedAtDesc(serviceId) —— 查出来就是全部评价;
//   加了"公开闸门"之后,【凡是给外人看的地方都只能取 APPROVED 的】。
//
//   所以这里故意把 auditStatus 做成【必传参数】而不是在方法里写死:
//   调用方签名上就必须回答"我要哪个状态的",想忘也忘不掉。
//   (如果图省事留一个不带状态的方法,早晚会有人直接调它,把待审核的评价放出去)
public interface EvaluationRepository extends JpaRepository<Evaluation, Long>,
        JpaSpecificationExecutor<Evaluation> {

    // 是否已评价(一单一评的幂等判断)
    boolean existsByOrderId(Long orderId);

    // 某用户自己写过的评价,按时间倒序(用户端"评价过了没"的按钮态就靠它)
    // 这里不过滤 auditStatus:用户要看得到自己待审 / 被驳回的评价
    List<Evaluation> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 某服务的评价列表,按时间倒序(服务详情页展示用)
    // ⚠️ 对外展示,调用方必须传 AuditStatus.APPROVED
    List<Evaluation> findByServiceIdAndAuditStatusOrderByCreatedAtDesc(Long serviceId, AuditStatus auditStatus);

    // 某商家的评价列表,按时间倒序(商家端"本店铺评价")
    // ⚠️ 同上,传给外人看的场合一律传 AuditStatus.APPROVED
    List<Evaluation> findByMerchantIdAndAuditStatusOrderByCreatedAtDesc(Long merchantId, AuditStatus auditStatus);

    // 统计:某商家某审核状态的评价数
    long countByMerchantIdAndAuditStatus(Long merchantId, AuditStatus auditStatus);

    // 统计:某商家某审核状态评价的平均分(无记录返回 null,由调用方按 0 处理)
    @Query("select avg(e.score) from Evaluation e where e.merchantId = :merchantId and e.auditStatus = :auditStatus")
    Double avgScoreByMerchantIdAndAuditStatus(@Param("merchantId") Long merchantId, @Param("auditStatus") AuditStatus auditStatus);
}
