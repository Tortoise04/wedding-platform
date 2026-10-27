package com.wedding.repository;

import com.wedding.entity.Penalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

// 冷库:违规处罚记录的存取。
//
// 额外继承 JpaSpecificationExecutor,管理端按 商家 / 处罚类型 / 生效状态 任意组合筛选时用它拼条件。
public interface PenaltyRepository extends JpaRepository<Penalty, Long>,
        JpaSpecificationExecutor<Penalty> {

    // 某商家的处罚历史,按处罚时间倒序(最新的在最前)
    List<Penalty> findByMerchantIdOrderByCreatedAtDesc(Long merchantId);
}
