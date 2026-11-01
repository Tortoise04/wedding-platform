package com.wedding.service;

import com.wedding.dto.CreatePenaltyRequest;
import com.wedding.entity.Penalty;
import com.wedding.enums.PenaltyType;

import java.util.List;

// 厨师:管理端违规处罚这道菜(需求 FR-A-01-4 / FR-A-01-3 / FR-A-03-5)。
public interface PenaltyService {

    // 全站处罚记录(FR-A-01-4):按商家 / 处罚类型 / 生效状态任意组合(都不传即全部)
    List<Penalty> listAllPenalties(Long merchantId, PenaltyType penaltyType, Integer status);

    // 某商家的处罚历史(按处罚时间倒序)
    List<Penalty> listByMerchant(Long merchantId);

    // 执行处罚(警告 / 服务下架 / 封禁):写罚单并按类型联动服务或店铺
    Penalty executePenalty(Long adminId, CreatePenaltyRequest request);

    // 解除处罚:恢复业务状态(仅当这张是最后一道生效处罚),并把 谁解的/何时/为何 留痕
    Penalty releasePenalty(Long adminId, Long penaltyId, String reason);
}
