package com.wedding.repository;

import com.wedding.entity.Dispute;
import com.wedding.enums.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 冷库:争议仲裁的存取。
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    // 按争议单号精确定位一单(单号全局唯一)
    Optional<Dispute> findByDisputeNo(String disputeNo);

    // 该订单是否已发起过争议(幂等:一个订单最多一次平台介入)
    boolean existsByOrderId(Long orderId);

    // 我的争议列表(用户端),按发起时间倒序
    List<Dispute> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 管理端"待受理争议"列表
    List<Dispute> findByStatusOrderByCreatedAtDesc(DisputeStatus status);

    // 管理端"全部争议"列表,按发起时间倒序
    List<Dispute> findAllByOrderByCreatedAtDesc();

    // 某商家被投诉的争议列表
    List<Dispute> findByMerchantIdOrderByCreatedAtDesc(Long merchantId);
}
