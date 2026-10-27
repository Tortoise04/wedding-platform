package com.wedding.controller;

import com.wedding.annotation.RequireAdmin;
import com.wedding.common.Result;
import com.wedding.dto.AcceptDisputeRequest;
import com.wedding.dto.ArbitrateDisputeRequest;
import com.wedding.entity.Dispute;
import com.wedding.entity.DisputeEvidence;
import com.wedding.enums.DisputeStatus;
import com.wedding.service.DisputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 服务员:管理端 · 争议仲裁(需求 FR-A-03)。
//
// 受理判断:符合条件就受理(转"举证中"),否则不予受理(直接关闭并写明原因)。
// ⚠️ 路径是 /api/admin/disputes,别写成用户端的 /api/disputes;争议有独立状态机,订单状态【不变】。
@RestController
@RequestMapping("/api/admin/disputes")
@RequiredArgsConstructor
public class AdminDisputeController {

    private final DisputeService disputeService;

    // 争议列表(管理端)—— status 传了就按状态筛(如 PENDING),不传返回全部;均按发起时间倒序
    @RequireAdmin
    @GetMapping
    public Result<List<Dispute>> list(@RequestParam(required = false) DisputeStatus status) {
        return Result.success(disputeService.listDisputes(status));
    }

    // 受理判断(管理端):approved=true 受理(→ 举证中);false 不予受理(→ 已关闭)
    @RequireAdmin
    @PostMapping("/{id}/accept")
    public Result<Dispute> accept(@PathVariable Long id,
                                  @RequestBody AcceptDisputeRequest request,
                                  @RequestAttribute("userId") Long adminId) {
        return Result.success(disputeService.acceptDispute(id, adminId, request));
    }

    // 举证材料汇总(管理端)—— 某争议下双方提交的全部材料,按时间正序
    @RequireAdmin
    @GetMapping("/{id}/evidences")
    public Result<List<DisputeEvidence>> evidences(@PathVariable Long id) {
        return Result.success(disputeService.listEvidences(id));
    }

    // 举证完毕(管理端)—— 管理员确认双方举证结束,转入"仲裁中"
    @RequireAdmin
    @PostMapping("/{id}/finish-evidence")
    public Result<Dispute> finishEvidence(@PathVariable Long id) {
        return Result.success(disputeService.finishEvidence(id));
    }

    // 作出裁决(管理端):落裁决结论并执行订单联动 —— REFUND / CLOSE_ORDER → 订单联动取消;REJECT → 订单不动。
    // ⚠️ 这一步【同时改多张表】,全程在一个事务里,详见 DisputeServiceImpl.arbitrate。
    @RequireAdmin
    @PostMapping("/{id}/arbitrate")
    public Result<Dispute> arbitrate(@PathVariable Long id,
                                     @RequestBody ArbitrateDisputeRequest request,
                                     @RequestAttribute("userId") Long adminId) {
        return Result.success(disputeService.arbitrate(id, adminId, request));
    }
}
