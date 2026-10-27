package com.wedding.repository;

import com.wedding.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 冷库:咨询会话的存取。
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    // 某用户与某商家的会话(一对只有一个,发起咨询时 get-or-create 用)
    Optional<Consultation> findByUserIdAndMerchantId(Long userId, Long merchantId);

    // 某用户针对"某商家的某服务"是否已有会话(门户重复点咨询时复用它,不再新建)
    Optional<Consultation> findByUserIdAndMerchantIdAndServiceId(Long userId, Long merchantId, Long serviceId);

    // 某用户的全部会话,按最近消息时间倒序
    List<Consultation> findByUserIdOrderByLastTimeDesc(Long userId);

    // 某商家的全部会话,按最近消息时间倒序
    List<Consultation> findByMerchantIdOrderByLastTimeDesc(Long merchantId);
}
