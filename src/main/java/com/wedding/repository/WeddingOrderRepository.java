package com.wedding.repository;

import com.wedding.entity.WeddingOrder;
import com.wedding.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

// 冷库:订单的存取。
//
// ⚠️ 额外继承 JpaSpecificationExecutor:管理端"全站订单"要按 状态 / 商家 / 用户 / 时间区间 任意组合筛选,
// 组合繁多写死方法名写不全,故改用 Specification 在 Service 里运行时拼条件。
public interface WeddingOrderRepository extends JpaRepository<WeddingOrder, Long>,
        JpaSpecificationExecutor<WeddingOrder> {

    // 行锁:取消 / 评价 / 发起介入时锁定订单行,让「状态与幂等检查 + 落库」在同一事务里串行。
    // 理由同上:事务提交在方法返回后,synchronized 挡不住未提交的并发读。
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from WeddingOrder o where o.id = :id")
    Optional<WeddingOrder> findByIdForUpdate(@Param("id") Long id);

    // 按订单号查(订单号全局唯一)
    Optional<WeddingOrder> findByOrderNo(String orderNo);

    // 我的订单列表,按下单时间倒序(用户端"我的订单")
    List<WeddingOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 幂等:该用户对该服务是否已存在某个状态的订单(用于重复下单校验)
    boolean existsByUserIdAndServiceIdAndStatus(Long userId, Long serviceId, OrderStatus status);

    // 档期互斥:同一服务同一天是否已有「未了结」的单(PENDING / CONFIRMED / IN_PROGRESS)。
    // 判据只认这几个状态,是因为取消 / 拒单 / 强制取消后这一天要重新放出来;
    // COMPLETED 的日期已过去,自然不会再撞上。
    boolean existsByServiceIdAndWeddingDateAndStatusIn(Long serviceId, LocalDate weddingDate,
                                                       Collection<OrderStatus> statuses);

    // 档期日历用:该服务下被「未了结」订单占用的全部婚礼日期,去重后一次取回。
    // 目的是让门户详情页在"选日期"那一刻就能把这些天灰掉,而不是等提交后拿 400。
    @Query("select distinct o.weddingDate from WeddingOrder o where o.serviceId = :serviceId and o.status in :statuses")
    List<LocalDate> findOccupiedWeddingDateByServiceIdAndStatusIn(@Param("serviceId") Long serviceId,
                                                                 @Param("statuses") Collection<OrderStatus> statuses);

    // 履约监督①:某状态下、下单时间早于某时刻的订单 —— 揪出"挂着不动"的滞留单
    // (例如 PENDING 超过 N 小时还没人接单);按下单时间正序,越早的排越前。
    List<WeddingOrder> findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(OrderStatus status, LocalDateTime time);

    // 履约监督②:某状态下、确认时间早于某时刻的订单 —— 揪出"接了单却迟迟不推进"的滞留单
    // (例如 CONFIRMED 超过 N 天还没开始履约);按确认时间正序。
    List<WeddingOrder> findByStatusAndConfirmedAtBeforeOrderByConfirmedAtAsc(OrderStatus status, LocalDateTime time);

    // 统计:某商家的订单数
    long countByMerchantId(Long merchantId);

    // 统计:某商家某状态的订单数
    long countByMerchantIdAndStatus(Long merchantId, OrderStatus status);

    // 统计:某状态的订单总数(平台订单状态分布用)
    long countByStatus(OrderStatus status);

    // 统计:某商家某状态订单的金额总和(无记录返回 null,由调用方按 0 处理)
    @Query("select sum(o.amount) from WeddingOrder o where o.merchantId = :merchantId and o.status = :status")
    BigDecimal sumAmountByMerchantIdAndStatus(@Param("merchantId") Long merchantId, @Param("status") OrderStatus status);

    // 统计:某状态订单的金额总和(全站;无记录返回 null)
    @Query("select sum(o.amount) from WeddingOrder o where o.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") OrderStatus status);

    // 统计:热门商家 —— 按订单量分组倒序,配合 Pageable 取前 N 名。
    // 返回每行是 [merchantId, 订单数] 两个值。
    @Query("select o.merchantId, count(o) from WeddingOrder o group by o.merchantId order by count(o) desc")
    List<Object[]> findTopMerchantsByOrderCount(Pageable pageable);

    // 统计:近7天订单量趋势(全站),按天分组。返回每行 [日期字符串, 订单数]。
    @Query(value = "SELECT DATE_FORMAT(created_at, '%Y-%m-%d') d, COUNT(*) FROM t_order WHERE created_at >= :since GROUP BY d",
            nativeQuery = true)
    List<Object[]> countOrdersByDaySince(@Param("since") LocalDateTime since);

    // 统计:近7天订单量趋势(本店),按天分组。
    @Query(value = "SELECT DATE_FORMAT(created_at, '%Y-%m-%d') d, COUNT(*) FROM t_order WHERE merchant_id = :merchantId AND created_at >= :since GROUP BY d",
            nativeQuery = true)
    List<Object[]> countOrdersByDaySinceAndMerchant(@Param("merchantId") Long merchantId, @Param("since") LocalDateTime since);
}
