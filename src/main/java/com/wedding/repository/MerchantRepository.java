package com.wedding.repository;

import com.wedding.entity.Merchant;
import com.wedding.enums.AuditStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

// 冷库:商家店铺的存取。
// 额外继承 JpaSpecificationExecutor,管理端关键字检索要按 店铺名 / 联系电话 + 审核状态 组合拼条件。
public interface MerchantRepository extends JpaRepository<Merchant, Long>,
        JpaSpecificationExecutor<Merchant> {

    // 按账号查店铺(一个账号最多一个店铺)
    Optional<Merchant> findByUserId(Long userId);

    // 该账号是否已提交过入驻申请(幂等判断)
    boolean existsByUserId(Long userId);

    // 管理端:按审核状态筛选的商家列表,申请时间倒序 + id 兜底(同秒顺序稳定)
    List<Merchant> findByAuditStatusOrderByCreatedAtDescIdDesc(AuditStatus auditStatus);

    // 管理端:全部商家列表,申请时间倒序 + id 兜底
    List<Merchant> findAllByOrderByCreatedAtDescIdDesc();

    // 门户"已入驻并通过审核 + 账号启用"的商家列表(游客可看,按申请时间倒序)
    List<Merchant> findByAuditStatusAndStatusOrderByCreatedAtDesc(AuditStatus auditStatus, Integer status);

    // 统计:某审核状态的商家数量
    long countByAuditStatus(AuditStatus auditStatus);
}
