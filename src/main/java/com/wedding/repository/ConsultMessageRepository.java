package com.wedding.repository;

import com.wedding.entity.ConsultMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 冷库:咨询消息的存取。
public interface ConsultMessageRepository extends JpaRepository<ConsultMessage, Long> {

    // 某会话的全部消息,按时间正序(从早到晚展示)
    List<ConsultMessage> findByConsultationIdOrderByCreatedAtAsc(Long consultationId);
}
