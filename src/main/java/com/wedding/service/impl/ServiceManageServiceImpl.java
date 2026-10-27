package com.wedding.service.impl;

import com.wedding.dto.CaseCreateRequest;
import com.wedding.dto.ScheduleSettingRequest;
import com.wedding.dto.ServicePublishRequest;
import com.wedding.entity.Merchant;
import com.wedding.entity.ServiceCase;
import com.wedding.entity.ServiceSchedule;
import com.wedding.entity.WeddingService;
import com.wedding.exception.BusinessException;
import com.wedding.repository.MerchantRepository;
import com.wedding.repository.ServiceCaseRepository;
import com.wedding.repository.ServiceScheduleRepository;
import com.wedding.repository.WeddingServiceRepository;
import com.wedding.service.MerchantService;
import com.wedding.service.ServiceManageService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// 厨师:商家服务管理(发布 / 编辑 / 上下架 / 档期 / 案例 / 审核)。
@Service
@RequiredArgsConstructor
public class ServiceManageServiceImpl implements ServiceManageService {
    private final WeddingServiceRepository weddingServiceRepository;
    private final ServiceScheduleRepository serviceScheduleRepository;
    private final ServiceCaseRepository serviceCaseRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantService merchantService;

    // 【商家端】发布服务:先解析"我是哪个商家",再建服务入库。
    //
    // ★ merchantId 由后端从当前用户解析,不信前端传的值。
    // 新服务一律 auditStatus = PENDING、shelfStatus = 0,等管理员审核后才可能上架。
    @Transactional
    @Override
    public WeddingService publish(Long userId, ServicePublishRequest request) {
        // 1. 解析"我是哪个商家"(★ 后端解析,不信前端)

        Merchant shop = merchantService.getMyShop(userId);

        // 1.5 入参校验:与 editService 保持一致,不能让空表单一路存进库报 500
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException("服务名称不能为空");
        }
        if (request.getCategoryId() == null) {
            throw new BusinessException("服务分类不能为空");
        }
        if (request.getPrice() == null) {
            throw new BusinessException("服务价格不能为空");
        }

        // 2. 建服务
        WeddingService s = new WeddingService();
        s.setMerchantId(shop.getId());          // ★ 后端解析
        s.setCategoryId(request.getCategoryId());
        s.setName(request.getName());
        s.setCoverUrl(request.getCoverUrl());
        s.setDescription(request.getDescription());
        s.setContent(request.getContent());
        s.setPrice(request.getPrice());
        s.setUnit(request.getUnit());
        s.setAuditStatus("PENDING");            // 等管理员审核
        s.setShelfStatus(0);                    // 默认下架
        s.setViewCount(0);
        s.setSalesCount(0);
        s.setScore(BigDecimal.ZERO);
        return weddingServiceRepository.save(s);
    }

    // 【商家端】上架 / 下架:★ 先做归属校验,非本店服务直接拒绝。
    @Transactional
    @Override
    public WeddingService toggleShelf(Long userId, Long serviceId, Integer shelfStatus){
        Merchant shop = merchantService.getMyShop(userId);                    // 我是哪个商家
        WeddingService s = weddingServiceRepository.findById(serviceId)
                .orElseThrow(() -> new BusinessException("服务不存在: " + serviceId));
        if (!s.getMerchantId().equals(shop.getId())) {                        // ★ 归属校验
            throw new BusinessException("无权操作其他店铺的服务");
        }
        s.setShelfStatus(shelfStatus);                                        // 1 上架 / 0 下架
        return weddingServiceRepository.save(s);
    };  // 商家上下架

    // 【管理端】审核服务:★ 只能审核 PENDING 的,已审核过的拒绝。
    @Transactional
    @Override
    public WeddingService auditService(Long serviceId, Boolean approved){
        WeddingService s = weddingServiceRepository.findById(serviceId)
                .orElseThrow(() -> new BusinessException("服务不存在: " + serviceId));
        // ★ 前置校验:只能审核待审核的
        if (!"PENDING".equals(s.getAuditStatus())) {
            throw new BusinessException("该服务已审核过");
        }
        s.setAuditStatus(Boolean.TRUE.equals(approved) ? "APPROVED" : "REJECTED");
        return weddingServiceRepository.save(s);
    };                 // 管理员审核服务

    // 【商家端】编辑服务:先归属校验,再改字段。
    //
    // ⚠️ 内容变了就退回 PENDING + 下架 —— 编辑过的服务必须重新过审核闸门,不能带着新内容直接对外。
    @Transactional
    @Override
    public WeddingService editService(Long userId, Long serviceId, ServicePublishRequest request) {
        Merchant shop = merchantService.getMyShop(userId);
        WeddingService s = weddingServiceRepository.findById(serviceId)
                .orElseThrow(() -> new BusinessException("服务不存在: " + serviceId));
        if (!s.getMerchantId().equals(shop.getId())) {
            throw new BusinessException("无权操作其他店铺的服务");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException("服务名称不能为空");
        }
        if (request.getPrice() == null) {
            throw new BusinessException("服务价格不能为空");
        }
        s.setCategoryId(request.getCategoryId());
        s.setName(request.getName());
        s.setCoverUrl(request.getCoverUrl());
        s.setDescription(request.getDescription());
        s.setContent(request.getContent());
        s.setPrice(request.getPrice());
        s.setUnit(request.getUnit());
        s.setAuditStatus("PENDING");   // 退回待审核
        s.setShelfStatus(0);           // 同时下架
        return weddingServiceRepository.save(s);
    }

    // 【商家端】设置档期:每个日期落一条"可预约"档期(FR-M-02-4)。
    @Transactional
    @Override
    public List<ServiceSchedule> addSchedules(Long userId, Long serviceId, ScheduleSettingRequest request) {
        Merchant shop = merchantService.getMyShop(userId);
        WeddingService s = weddingServiceRepository.findById(serviceId)
                .orElseThrow(() -> new BusinessException("服务不存在: " + serviceId));
        if (!s.getMerchantId().equals(shop.getId())) {
            throw new BusinessException("无权操作其他店铺的服务");
        }
        if (request.getDates() == null || request.getDates().isEmpty()) {
            throw new BusinessException("档期日期不能为空");
        }
        List<ServiceSchedule> result = new ArrayList<>();
        for (LocalDate date : request.getDates()) {
            if (date == null) {
                throw new BusinessException("档期日期不合法");
            }
            // 同一服务同一天只允许一条档期,重复提交直接跳过,不报错也不重复写
            if (serviceScheduleRepository.existsByServiceIdAndScheduleDate(serviceId, date)) {
                continue;
            }
            ServiceSchedule schedule = new ServiceSchedule();
            schedule.setServiceId(serviceId);
            schedule.setScheduleDate(date);
            schedule.setStatus(1);   // 1 可预约
            result.add(serviceScheduleRepository.save(schedule));
        }
        return result;
    }

    // 【商家端】查看已有档期:先归属校验,再按日期升序返回(前端从早到晚展示)。
    @Override
    public List<ServiceSchedule> listSchedules(Long userId, Long serviceId) {
        checkMyService(userId, serviceId);
        return serviceScheduleRepository.findByServiceIdOrderByScheduleDateAsc(serviceId);
    }

    // 【商家端】删除一条档期:同一套归属校验,再确认该档期确实挂在这条服务下。
    @Transactional
    @Override
    public void deleteSchedule(Long userId, Long serviceId, Long scheduleId) {
        checkMyService(userId, serviceId);
        ServiceSchedule schedule = serviceScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException("档期不存在: " + scheduleId));
        // 防止拿 A 服务的路径删掉 B 服务的档期
        if (!schedule.getServiceId().equals(serviceId)) {
            throw new BusinessException("该档期不属于该服务");
        }
        serviceScheduleRepository.delete(schedule);
    }

    // 归属校验(档期 / 案例多处复用):服务必须存在,且属于当前用户名下的店铺。
    private WeddingService checkMyService(Long userId, Long serviceId) {
        WeddingService s = weddingServiceRepository.findById(serviceId)
                .orElseThrow(() -> new BusinessException("服务不存在: " + serviceId));
        Merchant shop = merchantRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("请先完成商家入驻"));
        if (!s.getMerchantId().equals(shop.getId())) {
            throw new BusinessException("无权操作该服务");
        }
        return s;
    }

    // 【商家端】维护案例素材:新增一组案例(FR-M-02-6)。
    @Transactional
    @Override
    public ServiceCase addCase(Long userId, Long serviceId, CaseCreateRequest request) {
        Merchant shop = merchantService.getMyShop(userId);
        WeddingService s = weddingServiceRepository.findById(serviceId)
                .orElseThrow(() -> new BusinessException("服务不存在: " + serviceId));
        if (!s.getMerchantId().equals(shop.getId())) {
            throw new BusinessException("无权操作其他店铺的服务");
        }
        if (request.getImageUrl() == null || request.getImageUrl().isBlank()) {
            throw new BusinessException("案例图片不能为空");
        }
        String addVideoUrl = normalizeVideoUrl(request.getVideoUrl());
        ServiceCase c = new ServiceCase();
        c.setServiceId(serviceId);
        c.setImageUrl(request.getImageUrl());
        c.setVideoUrl(addVideoUrl);
        c.setTitle(request.getTitle());
        c.setDescription(request.getDescription());
        c.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        // 分类留空就存 null,不把空串写进库
        String addCategory = request.getCategory() == null ? null : request.getCategory().trim();
        c.setCategory(addCategory == null || addCategory.isEmpty() ? null : addCategory);
        return serviceCaseRepository.save(c);
    }

    // 【商家端】查看已有案例:先归属校验,再按 sortOrder 升序返回(前端展示顺序)。
    @Override
    public List<ServiceCase> listCases(Long userId, Long serviceId) {
        checkMyService(userId, serviceId);
        return serviceCaseRepository.findByServiceIdOrderBySortOrderAsc(serviceId);
    }

    // 【商家端】编辑案例:同一套归属校验,再确认该案例确实挂在这条服务下。
    @Transactional
    @Override
    public ServiceCase updateCase(Long userId, Long serviceId, Long caseId, CaseCreateRequest request) {
        checkMyService(userId, serviceId);
        ServiceCase c = serviceCaseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("案例不存在: " + caseId));
        // 防止拿 A 服务的路径改掉 B 服务的案例
        if (!c.getServiceId().equals(serviceId)) {
            throw new BusinessException("该案例不属于该服务");
        }
        if (request.getImageUrl() == null || request.getImageUrl().isBlank()) {
            throw new BusinessException("案例图片不能为空");
        }
        String updVideoUrl = normalizeVideoUrl(request.getVideoUrl());
        // 只覆盖内容六项,serviceId 与 createdAt 保持原值
        c.setImageUrl(request.getImageUrl());
        c.setVideoUrl(updVideoUrl);
        c.setTitle(request.getTitle());
        c.setDescription(request.getDescription());
        c.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        // 分类留空就存 null,不把空串写进库
        String updCategory = request.getCategory() == null ? null : request.getCategory().trim();
        c.setCategory(updCategory == null || updCategory.isEmpty() ? null : updCategory);
        return serviceCaseRepository.save(c);
    }

    // 【商家端】删除案例:同一套归属校验,再确认该案例确实挂在这条服务下。
    @Transactional
    @Override
    public void deleteCase(Long userId, Long serviceId, Long caseId) {
        checkMyService(userId, serviceId);
        ServiceCase c = serviceCaseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("案例不存在: " + caseId));
        if (!c.getServiceId().equals(serviceId)) {
            throw new BusinessException("该案例不属于该服务");
        }
        serviceCaseRepository.delete(c);
    }

    // 案例视频归一:空白一律存 null(不让空串进库),非空则必须是我们能播的三种容器。
    // 只认扩展名是因为上传白名单(《FileController》)也只按扩展名放行,两边口径一致才不会出现
    // 「传得上去但存不进来」或反过来。
    private String normalizeVideoUrl(String videoUrl) {
        if (videoUrl == null || videoUrl.isBlank()) {
            return null;
        }
        String trimmed = videoUrl.trim();
        String lower = trimmed.toLowerCase();
        if (!lower.endsWith(".mp4") && !lower.endsWith(".webm") && !lower.endsWith(".mov")) {
            throw new BusinessException("案例视频只支持 mp4/webm/mov");
        }
        return trimmed;
    }

    // 【商家端】本店铺服务:先解析"我是哪个商家",再按 shopId 分页查(含待审/下架)。
    @Override
    public Page<WeddingService> listMyServices(Long userId, int page, int size) {
        Merchant shop = merchantService.getMyShop(userId);
        return weddingServiceRepository.findByMerchantIdOrderByCreatedAtDescIdDesc(shop.getId(), PageRequest.of(page, size));
    }

    // 【管理端】服务列表:按内容审核状态筛选(不传 = 全部),发布时间倒序 + id 兜底。
    @Override
    public List<WeddingService> listServicesForAdmin(String auditStatus) {
        if (auditStatus != null && !auditStatus.isBlank()) {
            return weddingServiceRepository.findByAuditStatusOrderByCreatedAtDescIdDesc(auditStatus);
        }
        return weddingServiceRepository.findAllByOrderByCreatedAtDescIdDesc();
    }
}
