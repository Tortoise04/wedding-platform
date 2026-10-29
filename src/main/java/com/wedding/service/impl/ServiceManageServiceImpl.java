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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 厨师:商家服务管理(发布 / 编辑 / 上下架 / 档期 / 案例 / 审核)。
@Service
@RequiredArgsConstructor
public class ServiceManageServiceImpl implements ServiceManageService {

    // 时段白名单:前端下拉固定这四项,后端再兜一层,脏值不入库
    private static final java.util.Set<String> SLOTS = java.util.Set.of("整天", "上午", "下午", "晚间");

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
    // ★ 只有内容有实质变化才退回 PENDING + 下架;只补档期、或前端把同样的值原样回传这类
    //   无实质变化的保存,不碰审核状态与上架状态,库里原值保持不动。
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
        // 1) 回填之前先留一份旧值快照(★ 必须取在 set 之前,set 之后旧值就没了)
        Long oldCategoryId = s.getCategoryId();
        String oldName = s.getName();
        String oldCoverUrl = s.getCoverUrl();
        String oldDescription = s.getDescription();
        String oldContent = s.getContent();
        BigDecimal oldPrice = s.getPrice();
        String oldUnit = s.getUnit();
        // 2) 回填新值(与原来逐行一致)
        s.setCategoryId(request.getCategoryId());
        s.setName(request.getName());
        s.setCoverUrl(request.getCoverUrl());
        s.setDescription(request.getDescription());
        s.setContent(request.getContent());
        s.setPrice(request.getPrice());
        s.setUnit(request.getUnit());
        // 3) 逐字段比对新旧值,判断有没有实质变化。
        //    String / Long 用 null 安全的 Objects.equals(两边都 null 视为相同);
        //    price 用 compareTo —— 4.9 与 4.90 的 scale 不同但数值相等,不能算变化。
        boolean priceChanged;
        if (oldPrice == null) {
            priceChanged = (s.getPrice() != null);
        } else {
            priceChanged = (s.getPrice() == null) || oldPrice.compareTo(s.getPrice()) != 0;
        }
        boolean changed = priceChanged
                || !java.util.Objects.equals(oldCategoryId, s.getCategoryId())
                || !java.util.Objects.equals(oldName, s.getName())
                || !java.util.Objects.equals(oldCoverUrl, s.getCoverUrl())
                || !java.util.Objects.equals(oldDescription, s.getDescription())
                || !java.util.Objects.equals(oldContent, s.getContent())
                || !java.util.Objects.equals(oldUnit, s.getUnit());
        // 4) 真的改了内容才退回待审核 + 下架;没改就完全不碰这两个字段,保持库里原值
        if (changed) {
            s.setAuditStatus("PENDING");   // 退回待审核
            s.setShelfStatus(0);           // 同时下架
        }
        return weddingServiceRepository.save(s);
    }

    // 【商家端】设置档期:dates × slots 的笛卡尔积,每个组合落一条"可预约"档期(FR-M-02-4)。
    //
    // 老形态 {dates, slot, venue} 照样可用:slots 缺省时按 slot 单值处理。
    // 已存在的(同 serviceId + date + slot)跳过而不是报错,返回 created / skipped 两份明细。
    @Transactional
    @Override
    public Map<String, Object> addSchedules(Long userId, Long serviceId, ScheduleSettingRequest request) {
        Merchant shop = merchantService.getMyShop(userId);
        WeddingService s = weddingServiceRepository.findById(serviceId)
                .orElseThrow(() -> new BusinessException("服务不存在: " + serviceId));
        if (!s.getMerchantId().equals(shop.getId())) {
            throw new BusinessException("无权操作其他店铺的服务");
        }
        if (request.getDates() == null || request.getDates().isEmpty()) {
            throw new BusinessException("占用日期不能为空");
        }
        // 时段白名单:脏值直接拦下,别把"上午上"这种写进库(前端下拉固定四项)
        List<String> slotList = resolveSlots(request.getSlots(), request.getSlot());
        String venue = normalizeVenue(request.getVenue());
        // 日期先去重保序:一次提交里重复写同一天,不该落两条
        List<LocalDate> dateList = new ArrayList<>();
        for (LocalDate date : request.getDates()) {
            if (date == null) {
                throw new BusinessException("占用日期不合法");
            }
            if (!dateList.contains(date)) {
                dateList.add(date);
            }
        }
        List<ServiceSchedule> created = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();
        for (LocalDate date : dateList) {
            for (String slot : slotList) {
                // 同一服务同一天同一时段只允许一条:库里已有、或本轮刚写过,都算跳过。
                // 加时段之后,"10月1日上午"和"10月1日下午"是两条不同的占用,不能互相顶掉。
                boolean dup = serviceScheduleRepository.existsByServiceIdAndScheduleDateAndSlot(serviceId, date, slot)
                        || created.stream().anyMatch(c -> date.equals(c.getScheduleDate()) && slot.equals(c.getSlot()));
                if (dup) {
                    skipped.add(dateSlotDetail(date, slot));
                    continue;
                }
                ServiceSchedule schedule = new ServiceSchedule();
                schedule.setServiceId(serviceId);
                schedule.setScheduleDate(date);
                schedule.setSlot(slot);
                schedule.setVenue(venue);
                schedule.setStatus(1);   // 1 可预约
                created.add(serviceScheduleRepository.save(schedule));
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("created", created);
        data.put("skipped", skipped);
        data.put("createdCount", created.size());
        data.put("skippedCount", skipped.size());
        return data;
    }

    // 时段解析:slots 优先(多选),缺省回退老字段 slot,再缺省"整天";逐个过白名单并去重。
    private List<String> resolveSlots(List<String> slots, String slot) {
        List<String> raw = new ArrayList<>();
        if (slots != null) {
            for (String one : slots) {
                if (one != null && !one.isBlank()) {
                    raw.add(one.trim());
                }
            }
        }
        if (raw.isEmpty()) {
            raw.add(slot == null || slot.isBlank() ? "整天" : slot.trim());
        }
        List<String> result = new ArrayList<>();
        for (String one : raw) {
            if (!SLOTS.contains(one)) {
                throw new BusinessException("占用时段只能是:整天 / 上午 / 下午 / 晚间");
            }
            if (!result.contains(one)) {
                result.add(one);
            }
        }
        return result;
    }

    // 场地:空串按"没填"处理(Nullable),超过 100 字拒绝
    private String normalizeVenue(String venue) {
        if (venue == null) {
            return null;
        }
        String trimmed = venue.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 100) {
            throw new BusinessException("场地名称不能超过 100 个字符");
        }
        return trimmed;
    }

    // 被跳过的那一行:返回给前端的明细只有"日期 + 时段"
    private Map<String, Object> dateSlotDetail(LocalDate date, String slot) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("date", date.toString());
        detail.put("slot", slot);
        return detail;
    }

    // 【商家端】查看已有档期:先归属校验,再按日期升序返回(前端从早到晚展示)。
    @Override
    public List<ServiceSchedule> listSchedules(Long userId, Long serviceId) {
        checkMyService(userId, serviceId);
        return serviceScheduleRepository.findByServiceIdOrderByScheduleDateAsc(serviceId);
    }

    // 【商家端】改单条档期的时段 / 场地:日期不可改(改日期等于换一条档期)。
    //
    // ★ status != 1 说明这天已被订单占用,改时段会让订单对不上,直接拒绝。
    @Transactional
    @Override
    public ServiceSchedule updateSchedule(Long userId, Long serviceId, Long scheduleId, ScheduleSettingRequest request) {
        checkMyService(userId, serviceId);
        ServiceSchedule schedule = serviceScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException("档期不存在: " + scheduleId));
        if (!schedule.getServiceId().equals(serviceId)) {
            throw new BusinessException("该档期不属于该服务");
        }
        if (schedule.getStatus() == null || schedule.getStatus() != 1) {
            throw new BusinessException("该日期已被订单占用，不能修改或删除");
        }
        // 时段不传/传空 = 保持原时段,避免前端漏字段把"下午"悄悄改回"整天"
        String slot = schedule.getSlot();
        if (request != null && request.getSlot() != null && !request.getSlot().isBlank()) {
            slot = request.getSlot().trim();
        }
        if (!SLOTS.contains(slot)) {
            throw new BusinessException("占用时段只能是:整天 / 上午 / 下午 / 晚间");
        }
        // 改成别的时段时,同一天该时段若已有占用,唯一键会炸 500,先拦成可读文案
        if (!slot.equals(schedule.getSlot())
                && serviceScheduleRepository.existsByServiceIdAndScheduleDateAndSlot(serviceId, schedule.getScheduleDate(), slot)) {
            throw new BusinessException("该日期此时段已有占用,请换一个时段或先删除原占用");
        }
        schedule.setSlot(slot);
        if (request != null) {
            // 场地是整体覆盖:前端清空 = 改成没场地
            schedule.setVenue(normalizeVenue(request.getVenue()));
        }
        return serviceScheduleRepository.save(schedule);
    }

    // 【商家端】删除一条档期:同一套归属校验,再确认该档期确实挂在这条服务下、且没被订单占用。
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
        if (schedule.getStatus() == null || schedule.getStatus() != 1) {
            throw new BusinessException("该日期已被订单占用，不能修改或删除");
        }
        serviceScheduleRepository.delete(schedule);
    }

    // 【商家端】批量删除档期:只删传入的 id,返回真正删掉的条数。
    //
    // 整批要么全成功要么全不动(同一个事务):碰到一条被订单占用的就整批拒绝,
    // 免得前端勾了 10 条、悄悄删掉 9 条再报错,用户以为一条都没删。
    @Transactional
    @Override
    public int deleteSchedules(Long userId, Long serviceId, List<Long> scheduleIds) {
        checkMyService(userId, serviceId);
        List<ServiceSchedule> targets = new ArrayList<>();
        if (scheduleIds != null) {
            for (Long scheduleId : scheduleIds) {
                if (scheduleId == null) {
                    continue;
                }
                ServiceSchedule schedule = serviceScheduleRepository.findById(scheduleId)
                        .orElseThrow(() -> new BusinessException("档期不存在: " + scheduleId));
                if (!schedule.getServiceId().equals(serviceId)) {
                    throw new BusinessException("该档期不属于该服务");
                }
                if (schedule.getStatus() == null || schedule.getStatus() != 1) {
                    throw new BusinessException("该日期已被订单占用，不能修改或删除");
                }
                if (targets.stream().noneMatch(t -> t.getId().equals(schedule.getId()))) {
                    targets.add(schedule);
                }
            }
        }
        if (targets.isEmpty()) {
            throw new BusinessException("请选择要删除的档期");
        }
        serviceScheduleRepository.deleteAll(targets);
        return targets.size();
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
        c.setDescription(normalizeCaseDescription(request.getDescription()));
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
        c.setDescription(normalizeCaseDescription(request.getDescription()));
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

    // 案例说明归一:空白存 null;超过 500 字直接拦下(库里是 varchar(500),不拦会落成 500 报错)。
    // 说明允许写详细内容(方案、布置、流程),前端是多行输入框,这里只做长度护栏。
    private String normalizeCaseDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String d = description.trim();
        if (d.length() > 500) {
            throw new BusinessException("案例说明不能超过 500 个字符");
        }
        return d;
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
