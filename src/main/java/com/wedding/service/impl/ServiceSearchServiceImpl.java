package com.wedding.service.impl;

import com.wedding.dto.ServiceSearchQuery;
import com.wedding.entity.BrowseHistory;
import com.wedding.entity.Evaluation;
import com.wedding.entity.ServiceCase;
import com.wedding.entity.ServiceSchedule;
import com.wedding.entity.User;
import com.wedding.entity.WeddingService;
import com.wedding.enums.AuditStatus;
import com.wedding.enums.OrderStatus;
import com.wedding.exception.BusinessException;
import com.wedding.repository.BrowseHistoryRepository;
import com.wedding.repository.EvaluationRepository;
import com.wedding.repository.ServiceCaseRepository;
import com.wedding.repository.ServiceScheduleRepository;
import com.wedding.repository.UserRepository;
import com.wedding.repository.WeddingOrderRepository;
import com.wedding.repository.WeddingServiceRepository;
import com.wedding.service.ServiceSearchService;
import com.wedding.spec.ServiceSpecs;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

// 厨师:实现"服务检索"这道菜。
//
// Specification 真正落地的地方:条件在 ServiceSpecs 里,组合在这里,交给 findAll()。
@Service
@RequiredArgsConstructor
public class ServiceSearchServiceImpl implements ServiceSearchService {

    // 该仓储继承了 JpaSpecificationExecutor,能接收 Specification
    private final WeddingServiceRepository weddingServiceRepository;
    private final ServiceCaseRepository serviceCaseRepository;
    private final ServiceScheduleRepository serviceScheduleRepository;
    private final BrowseHistoryRepository browseHistoryRepository;
    private final WeddingOrderRepository weddingOrderRepository;
    // 门户评价接口要读评价与用户姓名(姓名只用来算脱敏标签,不对外返回)
    private final EvaluationRepository evaluationRepository;
    private final UserRepository userRepository;

    // 「占住档期」的订单状态:与 OrderServiceImpl.OCCUPYING_STATUS 同一判据 ——
    // 取消 / 拒单 / 强制取消后这一天要重新放出来,COMPLETED 的日期已过去。
    private static final List<OrderStatus> OCCUPYING_STATUS =
            List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.IN_PROGRESS);

    // 门户搜索:默认只查上架的,再按关键词 / 分类 / 价格 / 商家逐条叠加条件,分页返回。
    @Override
    public Page<WeddingService> search(ServiceSearchQuery query) {
        Specification<WeddingService> spec = Specification.where(ServiceSpecs.shelfOn());   // 上架查询

        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            spec = spec.and(ServiceSpecs.keywordLike(query.getKeyword()));           // 关键词
        }
        if (query.getCategoryId() != null) {
            spec = spec.and(ServiceSpecs.categoryEquals(query.getCategoryId()));     // 分类
        }
        if (query.getPriceMin() != null) {
            spec = spec.and(ServiceSpecs.priceGreaterThanOrEqual(query.getPriceMin())); // 价格下限
        }
        if (query.getPriceMax() != null) {
            spec = spec.and(ServiceSpecs.priceLessThanOrEqual(query.getPriceMax()));    // 价格上限
        }
        if (query.getMerchantId() != null) {
            spec = spec.and(ServiceSpecs.merchantEquals(query.getMerchantId()));    // 商家
        }
        if (query.getBookableDate() != null) {
            spec = spec.and(ServiceSpecs.hasBookableDate(query.getBookableDate())); // 可约日期
        }

        Sort sort = resolveSort(query.getSort());
        PageRequest pageable = PageRequest.of(
                query.getPage() == null ? 0 : query.getPage(),
                query.getSize() == null ? 10 : query.getSize(),
                sort);

        return weddingServiceRepository.findAll(spec, pageable);
    }

    // 本页每张卡片的「最近可约」概览 —— 一次 group by 查完整页,不按卡片循环查询。
    //
    // 用 group by 而不是把查询塞进 search() 里:search 只该管"筛出哪些服务",
    // 「最近可约 / 可约天数」是列表展示用的附属信息,按服务 id 批量取一次就够。
    //   返回 { serviceId: {bookableFrom: "YYYY-MM-DD", bookableCount: N} },只含未来(status=1 且 >= 今天)的档期。
    // 用 JPQL 直查而不是加仓储方法:仓储就是"按方法名生成查询",这种聚合查询写在那里会把仓储撑成一个杂货铺。
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Map<String, Object>> bookableSummary(Collection<Long> serviceIds) {
        Map<String, Map<String, Object>> summary = new LinkedHashMap<>();
        if (serviceIds == null || serviceIds.isEmpty()) {
            return summary;
        }
        // min(最早一天) + count(distinct 日期) —— 同一天上午/下午两条也只算一天
        String jpql = "select s.serviceId, min(s.scheduleDate), count(distinct s.scheduleDate) "
                + "from ServiceSchedule s "
                + "where s.serviceId in :ids and s.status = 1 and s.scheduleDate >= :today "
                + "group by s.serviceId";
        List<Object[]> rows = entityManager.createQuery(jpql, Object[].class)
                .setParameter("ids", serviceIds)
                .setParameter("today", LocalDate.now())
                .getResultList();
        for (Object[] row : rows) {
            Long serviceId = ((Number) row[0]).longValue();
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("bookableFrom", ((LocalDate) row[1]).format(DateTimeFormatter.ISO_LOCAL_DATE));
            one.put("bookableCount", ((Number) row[2]).longValue());
            summary.put(String.valueOf(serviceId), one);
        }
        return summary;
    }

    // 把前端传的业务排序名映射成数据库排序字段:
    //   newest(默认)→createdAt desc · priceAsc→price asc · priceDesc→price desc · hot→salesCount desc
    // 前端只碰业务名,碰不到列名,也就没有注入口子。
    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sort) {
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "price");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "price");
            case "hot" -> Sort.by(Sort.Direction.DESC, "salesCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
    // 服务详情:把主服务 + 案例 + 档期三块拼成一个 Map 返回给前端。
    // 登录用户顺带记一条浏览记录(游客不记,FR-U-06-3)。
    @Override
    public Map<String, Object> getDetail(Long id, Long userId){
        // 主服务,查不到即报错
        WeddingService service = weddingServiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("服务不存在: " + id));

        // 记录浏览:只有登录用户才记(公开接口,游客 userId 为 null)
        if (userId != null) {
            BrowseHistory history = new BrowseHistory();
            history.setUserId(userId);
            history.setServiceId(id);
            browseHistoryRepository.save(history);
        }

        // 案例,按 sortOrder 升序
        List<ServiceCase> cases = serviceCaseRepository.findByServiceIdOrderBySortOrderAsc(id);
        // 档期,按档期日期升序
        List<ServiceSchedule> schedules =serviceScheduleRepository.findByServiceIdOrderByScheduleDateAsc(id);
        // 可约日期 = 这一天还有 status=1 的时段(同一天上午可约就算整天可约,订单只按天占,不拆时段)。
        // 已占用日期 = 这一天所有时段都不可约(status 全非 1) ∪ 未了结订单占用的婚礼日期。
        // 前端日历两用:不在「可约」里、或在「已占用」里的日子一律灰掉。
        // 用 TreeSet 是因为两路来源会重叠(下单时档期表被置 0,同时订单也占着这一天)。
        Set<LocalDate> bookable = new TreeSet<>();
        for (ServiceSchedule s : schedules) {
            if (s.getScheduleDate() != null && s.getStatus() != null && s.getStatus() == 1) {
                bookable.add(s.getScheduleDate());
            }
        }
        Set<LocalDate> occupied = new TreeSet<>();
        for (ServiceSchedule s : schedules) {
            if (s.getScheduleDate() != null && !bookable.contains(s.getScheduleDate())) {
                occupied.add(s.getScheduleDate());
            }
        }
        occupied.addAll(weddingOrderRepository.findOccupiedWeddingDateByServiceIdAndStatusIn(id, OCCUPYING_STATUS));

        // 一条档期都没维护的服务按天报价,日历不限日期(前端靠这个开关决定要不要"只放开有档期的日子")
        boolean hasSchedule = !schedules.isEmpty();

        // 五块一起返回(service / cases / schedules / occupiedDates 是老键,前端与测试都在用,不能改名)
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", service);
        result.put("cases", cases);
        result.put("schedules", schedules);
        result.put("occupiedDates", toIsoDates(occupied));
        result.put("bookableDates", toIsoDates(bookable));
        result.put("hasSchedule", hasSchedule);
        return result;
    }

    // 门户 · 某服务的公开评价(游客可访问):只放已通过审核的,列表按时间倒序取最近 20 条。
    //
    // 聚合三个数(count / average / distribution)用【一条】group by 查完:
    // 按 score 分组只拿到"每档几条",总数在内存里累加、平均分用(分数×条数)求和后再除,
    // 不能为 5 个星级各查一次,那样一个游客打开详情页就是 5 条一模一样的 SQL。
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> publicEvaluations(Long serviceId) {
        // ① 一条 JPQL:评分 → 该评分的条数
        String jpql = "select e.score, count(e) from Evaluation e "
                + "where e.serviceId = :serviceId and e.auditStatus = :auditStatus "
                + "group by e.score";
        List<Object[]> rows = entityManager.createQuery(jpql, Object[].class)
                .setParameter("serviceId", serviceId)
                .setParameter("auditStatus", AuditStatus.APPROVED)
                .getResultList();

        // 固定输出 5→1 五档,没有评价的星级补 0(前端画星级分布时不用再判空)
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            distribution.put(String.valueOf(star), 0L);
        }
        long count = 0L;
        long scoreSum = 0L;                    // 分数 × 条数,用来算平均分
        for (Object[] row : rows) {
            int score = ((Number) row[0]).intValue();
            long num = ((Number) row[1]).longValue();
            count += num;
            scoreSum += (long) score * num;
            distribution.put(String.valueOf(score), num);
        }
        // 平均分保留 1 位小数(四舍五入);一条已通过评价都没有时给 0.0
        double average = count == 0 ? 0.0
                : BigDecimal.valueOf((double) scoreSum / count).setScale(1, RoundingMode.HALF_UP).doubleValue();

        // ② 最近 20 条:仓储按时间倒序全取 APPROVED,在内存里截断(条数不大,不值得再写一个分页方法)
        List<Evaluation> approved = evaluationRepository
                .findByServiceIdAndAuditStatusOrderByCreatedAtDesc(serviceId, AuditStatus.APPROVED);
        List<Evaluation> latest = approved.size() > 20 ? approved.subList(0, 20) : approved;

        // ③ 这最多 20 条的用户名一次批量取,不在循环里按 userId 逐条查
        Set<Long> userIds = new LinkedHashSet<>();
        for (Evaluation evaluation : latest) {
            userIds.add(evaluation.getUserId());
        }
        Map<Long, String> labels = new LinkedHashMap<>();
        if (!userIds.isEmpty()) {
            for (User user : userRepository.findAllById(userIds)) {
                labels.put(user.getId(), maskName(user));
            }
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Evaluation evaluation : latest) {
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("id", evaluation.getId());
            one.put("score", evaluation.getScore());
            one.put("content", evaluation.getContent());
            one.put("images", evaluation.getImages());
            one.put("videoUrl", evaluation.getVideoUrl());
            one.put("replyContent", evaluation.getReplyContent());
            one.put("replyTime", evaluation.getReplyTime());
            one.put("createdAt", evaluation.getCreatedAt());
            // 只给标签:userId / orderId / 完整姓名一律不出接口
            one.put("userLabel", userLabel(evaluation, labels));
            list.add(one);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", count);
        result.put("average", average);
        result.put("distribution", distribution);
        result.put("list", list);
        return result;
    }

    // 评价人标签:匿名的一律「匿名用户」,其余用脱敏后的姓名
    private String userLabel(Evaluation evaluation, Map<Long, String> labels) {
        if (Boolean.TRUE.equals(evaluation.getAnonymous())) {
            return "匿名用户";
        }
        String masked = labels.get(evaluation.getUserId());
        return masked == null ? "匿名用户" : masked;   // 用户被删 / 姓名算不出来时也按匿名兜底
    }

    // 姓名脱敏:只留第一个字,后面补两个星号(「张**」)。真实姓名没填就退到登录名,两者都空则返回 null。
    private String maskName(User user) {
        String name = user.getRealName();
        if (name == null || name.isBlank()) {
            name = user.getUsername();
        }
        if (name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.substring(0, 1) + "**";
    }

    private List<String> toIsoDates(Set<LocalDate> dates) {
        return dates.stream().map(d -> d.format(DateTimeFormatter.ISO_LOCAL_DATE)).toList();
    }

}
