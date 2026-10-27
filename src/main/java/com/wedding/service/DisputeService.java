package com.wedding.service;

import com.wedding.dto.AcceptDisputeRequest;
import com.wedding.dto.ArbitrateDisputeRequest;
import com.wedding.dto.CreateDisputeRequest;
import com.wedding.dto.SubmitEvidenceRequest;
import com.wedding.entity.Dispute;
import com.wedding.entity.DisputeEvidence;
import com.wedding.enums.DisputeStatus;

import java.util.List;

// 厨师:争议与举证这道菜(需求 FR-A-03)。
public interface DisputeService {

    // 用户发起平台介入(创建争议)
    Dispute createDispute(Long userId, CreateDisputeRequest request);

    // 用户端:我的争议列表,按发起时间倒序
    List<Dispute> listMyDisputes(Long userId);

    // 商家端:针对本店的争议列表,按发起时间倒序
    List<Dispute> listShopDisputes(Long userId);

    // 当事人查看争议详情(发起人或被投诉店铺店主,非当事人抛业务异常)
    Dispute getMyDispute(Long disputeId, Long userId);

    // 当事人查看该争议的全部举证材料(按提交时间正序,前端当聊天流用)
    List<DisputeEvidence> listMyEvidences(Long disputeId, Long userId);

    // 管理端:争议列表(status 为 null 时返回全部)
    List<Dispute> listDisputes(DisputeStatus status);

    // 管理端:受理判断(approved=true 受理 → 举证中;false 不予受理 → 已关闭)
    Dispute acceptDispute(Long disputeId, Long adminId, AcceptDisputeRequest request);

    // 双方举证:当事人(用户/商家)提交一份举证材料,仅"举证中"可提交
    DisputeEvidence submitEvidence(Long disputeId, Long submitterId, SubmitEvidenceRequest request);

    // 双方举证:查看某争议的全部举证材料(按提交时间正序,双方材料并列)
    List<DisputeEvidence> listEvidences(Long disputeId);

    // 双方举证:管理员确认双方举证完毕 → 转入"仲裁中"(PROCESSING → ARBITRATING)
    Dispute finishEvidence(Long disputeId);

    // 作出裁决:落裁决结论并执行订单联动(REFUND/CLOSE_ORDER → 订单取消;REJECT → 订单不动),争议 → 已裁决
    Dispute arbitrate(Long disputeId, Long adminId, ArbitrateDisputeRequest request);
}
