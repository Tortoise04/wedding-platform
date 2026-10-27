package com.wedding.repository;

import com.wedding.entity.OrderStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 冷库:订单状态流转记录的存取。
public interface OrderStatusLogRepository extends JpaRepository<OrderStatusLog, Long> {

    // 某订单的完整流转轨迹,按时间正序(从早到晚)
    List<OrderStatusLog> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
