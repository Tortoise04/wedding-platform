package com.wedding.service;

import com.wedding.dto.CaseCreateRequest;
import com.wedding.dto.ScheduleSettingRequest;
import com.wedding.dto.ServicePublishRequest;
import com.wedding.entity.ServiceCase;
import com.wedding.entity.ServiceSchedule;
import com.wedding.entity.WeddingService;
import org.springframework.data.domain.Page;

import java.util.List;


// 厨师:服务管理这道菜 —— 商家发布/编辑/上下架/档期/案例,管理员审核。
public interface ServiceManageService {
    // 商家发布服务(初始 auditStatus=PENDING、shelfStatus=0,待审核)
    WeddingService publish(Long userId, ServicePublishRequest request);
    // 商家自助上下架(shelfStatus:1 上架 / 0 下架)
    WeddingService toggleShelf(Long userId, Long serviceId, Integer shelfStatus);
    // 管理员审核服务(approved=true → APPROVED;false → REJECTED)
    WeddingService auditService(Long serviceId, Boolean approved);
    // 商家编辑服务(内容变更后退回 PENDING + 下架,重新走审核)
    WeddingService editService(Long userId, Long serviceId, ServicePublishRequest request);
    // 商家设置档期(dates × slots 笛卡尔积,已存在的跳过;返回 created / skipped 明细)
    java.util.Map<String, Object> addSchedules(Long userId, Long serviceId, ScheduleSettingRequest request);
    // 商家端:某条服务已有的档期(按日期升序)
    List<ServiceSchedule> listSchedules(Long userId, Long serviceId);
    // 商家端:改单条档期的时段/场地(仅 status=1 可改)
    ServiceSchedule updateSchedule(Long userId, Long serviceId, Long scheduleId, ScheduleSettingRequest request);
    // 商家端:删除某条服务的一条档期(仅 status=1 可删)
    void deleteSchedule(Long userId, Long serviceId, Long scheduleId);
    // 商家端:批量删除档期,返回真正删除的条数(其中任一条被订单占用则整批拒绝)
    int deleteSchedules(Long userId, Long serviceId, List<Long> scheduleIds);
    // 商家维护案例素材(新增一组案例)
    ServiceCase addCase(Long userId, Long serviceId, CaseCreateRequest request);
    // 商家端:某条服务已有的案例(按 sortOrder 升序)
    List<ServiceCase> listCases(Long userId, Long serviceId);
    // 商家端:编辑某条案例(只改图片/标题/说明/排序,不动 serviceId 与 createdAt)
    ServiceCase updateCase(Long userId, Long serviceId, Long caseId, CaseCreateRequest request);
    // 商家端:删除某条案例
    void deleteCase(Long userId, Long serviceId, Long caseId);
    // 商家端:本店铺服务(含待审/下架),分页返回(page 从 0 起,size 每页条数)
    Page<WeddingService> listMyServices(Long userId, int page, int size);
    // 管理端:服务列表(按内容审核状态筛选,不传 = 全部)
    List<WeddingService> listServicesForAdmin(String auditStatus);
}
