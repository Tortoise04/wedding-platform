package com.wedding.controller;

import com.wedding.annotation.RequireMerchant;
import com.wedding.common.Result;
import com.wedding.dto.CaseCreateRequest;
import com.wedding.dto.ScheduleSettingRequest;
import com.wedding.dto.ServicePublishRequest;
import com.wedding.entity.ServiceCase;
import com.wedding.entity.ServiceSchedule;
import com.wedding.entity.WeddingService;
import com.wedding.service.ServiceManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


// 服务员:商家端 · 服务管理(路径 /api/merchant/services,均 @RequireMerchant,只能操作本店服务)。
@RestController
@RequestMapping("/api/merchant/services")
@RequiredArgsConstructor
public class MerchantServiceController {
    private final ServiceManageService serviceManageService;
    // 本店铺服务列表(含待审/下架),分页,返回 {list, total, totalPages, page, size}
    @RequireMerchant
    @GetMapping
    public Result<Map<String, Object>> list(@RequestAttribute("userId") Long userId,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        Page<WeddingService> p = serviceManageService.listMyServices(userId, page, size);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", p.getContent());
        data.put("total", p.getTotalElements());
        data.put("totalPages", p.getTotalPages());
        data.put("page", p.getNumber());
        data.put("size", p.getSize());
        return Result.success(data);
    }
    // 商家发布服务(初始 auditStatus=PENDING、shelfStatus=0,待管理员审核)
    @RequireMerchant
    @PostMapping
    public Result<WeddingService> publish(@RequestBody ServicePublishRequest request,
                                          @RequestAttribute("userId") Long userId) {
        return Result.success(serviceManageService.publish(userId, request));
    }
    // 商家编辑服务(内容变更后退回待审核 + 下架)
    @RequireMerchant
    @PutMapping("/{id}")
    public Result<WeddingService> edit(@PathVariable Long id,
                                       @RequestBody ServicePublishRequest request,
                                       @RequestAttribute("userId") Long userId) {
        return Result.success(serviceManageService.editService(userId, id, request));
    }
    // 商家自助上下架(shelfStatus:1 上架 / 0 下架)
    @RequireMerchant
    @PostMapping("/{id}/shelf")
    public Result<WeddingService> shelf(@PathVariable Long id,
                                        @RequestParam Integer shelfStatus,
                                        @RequestAttribute("userId") Long userId) {
        return Result.success(serviceManageService.toggleShelf(userId, id, shelfStatus));
    }
    // 商家设置档期(一批日期,每个日期一条可预约档期)
    @RequireMerchant
    @PostMapping("/{id}/schedules")
    public Result<List<ServiceSchedule>> schedules(@PathVariable Long id,
                                                   @RequestBody ScheduleSettingRequest request,
                                                   @RequestAttribute("userId") Long userId) {
        return Result.success(serviceManageService.addSchedules(userId, id, request));
    }
    // 商家查看该服务已有的档期(按日期升序),供弹窗列表展示
    @RequireMerchant
    @GetMapping("/{id}/schedules")
    public Result<List<ServiceSchedule>> listSchedules(@PathVariable Long id,
                                                       @RequestAttribute("userId") Long userId) {
        return Result.success(serviceManageService.listSchedules(userId, id));
    }
    // 商家删除该服务的一条档期
    @RequireMerchant
    @DeleteMapping("/{id}/schedules/{scheduleId}")
    public Result<Void> deleteSchedule(@PathVariable Long id,
                                       @PathVariable Long scheduleId,
                                       @RequestAttribute("userId") Long userId) {
        serviceManageService.deleteSchedule(userId, id, scheduleId);
        return Result.success(null);
    }
    // 商家维护案例素材(新增一组案例)
    @RequireMerchant
    @PostMapping("/{id}/cases")
    public Result<ServiceCase> cases(@PathVariable Long id,
                                     @RequestBody CaseCreateRequest request,
                                     @RequestAttribute("userId") Long userId) {
        return Result.success(serviceManageService.addCase(userId, id, request));
    }
    // 商家查看该服务已有的案例(按 sortOrder 升序),供弹窗列表展示
    @RequireMerchant
    @GetMapping("/{id}/cases")
    public Result<List<ServiceCase>> listCases(@PathVariable Long id,
                                               @RequestAttribute("userId") Long userId) {
        return Result.success(serviceManageService.listCases(userId, id));
    }
    // 商家编辑该服务下的一条案例(案例必须真属于该服务)
    @RequireMerchant
    @PutMapping("/{id}/cases/{caseId}")
    public Result<ServiceCase> updateCase(@PathVariable Long id,
                                          @PathVariable Long caseId,
                                          @RequestBody CaseCreateRequest request,
                                          @RequestAttribute("userId") Long userId) {
        return Result.success(serviceManageService.updateCase(userId, id, caseId, request));
    }
    // 商家删除该服务下的一条案例
    @RequireMerchant
    @DeleteMapping("/{id}/cases/{caseId}")
    public Result<Void> deleteCase(@PathVariable Long id,
                                   @PathVariable Long caseId,
                                   @RequestAttribute("userId") Long userId) {
        serviceManageService.deleteCase(userId, id, caseId);
        return Result.success(null);
    }
}
