package com.wedding.repository;

import com.wedding.entity.WeddingService;
import org.springframework.data.domain.Page;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// 冷库:服务的存取。
//
// ⚠️ 继承两个接口:JpaRepository 给基础增删改查,JpaSpecificationExecutor(★ 阶段 2 新增)是"能用 Specification 查询"的开关。
// 它带来 findAll(Specification[, Pageable]) 与 count(Specification),没有它传不进 Specification。
public interface WeddingServiceRepository
        extends JpaRepository<WeddingService, Long>,
                JpaSpecificationExecutor<WeddingService> {

    // 行锁:下单时锁定服务行,把「幂等检查 + 建单」串行化。
    // 只靠 synchronized 不够 —— Spring 的事务在方法返回后才提交,
    // 先到的请求还没 commit,后到的请求读不到它写的行,仍会重复建单。
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from WeddingService s where s.id = :id")
    java.util.Optional<WeddingService> findByIdForUpdate(@Param("id") Long id);

    // 管理端:按内容审核状态筛选的服务列表,发布时间倒序 + id 兜底
    List<WeddingService> findByAuditStatusOrderByCreatedAtDescIdDesc(String auditStatus);

    // 管理端:全部服务列表,发布时间倒序 + id 兜底
    List<WeddingService> findAllByOrderByCreatedAtDescIdDesc();

    // 商家端:本店铺服务(含待审/下架),发布时间倒序 + id 兜底,分页
    Page<WeddingService> findByMerchantIdOrderByCreatedAtDescIdDesc(Long merchantId, Pageable pageable);

    // 热门服务:按成交量倒序前 5(平台统计用)
    List<WeddingService> findTop5ByOrderBySalesCountDesc();

    // 统计:某商家的服务数
    long countByMerchantId(Long merchantId);

    // 统计:某商家已上架的服务数
    long countByMerchantIdAndShelfStatus(Long merchantId, Integer shelfStatus);

    // 统计:某商家全部服务的浏览量总和(无服务时返回 null,由调用方按 0 处理)
    @Query("select sum(s.viewCount) from WeddingService s where s.merchantId = :merchantId")
    Long sumViewCountByMerchantId(@Param("merchantId") Long merchantId);
}
