package com.wedding.repository;

import com.wedding.entity.DisputeEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 冷库:争议举证材料的存取。
public interface DisputeEvidenceRepository extends JpaRepository<DisputeEvidence, Long> {

    // 某争议的全部举证材料,按提交时间正序(用户与商家的材料并列)
    List<DisputeEvidence> findByDisputeIdOrderByCreatedAtAsc(Long disputeId);
}
