package com.wedding.service.impl;

import com.wedding.dto.ServiceSearchQuery;
import com.wedding.entity.BrowseHistory;
import com.wedding.entity.ServiceCase;
import com.wedding.entity.ServiceSchedule;
import com.wedding.entity.WeddingService;
import com.wedding.enums.OrderStatus;
import com.wedding.exception.BusinessException;
import com.wedding.repository.BrowseHistoryRepository;
import com.wedding.repository.ServiceCaseRepository;
import com.wedding.repository.ServiceScheduleRepository;
import com.wedding.repository.WeddingOrderRepository;
import com.wedding.repository.WeddingServiceRepository;
import com.wedding.service.ServiceSearchService;
import com.wedding.spec.ServiceSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
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

        Sort sort = resolveSort(query.getSort());
        PageRequest pageable = PageRequest.of(
                query.getPage() == null ? 0 : query.getPage(),
                query.getSize() == null ? 10 : query.getSize(),
                sort);

        return weddingServiceRepository.findAll(spec, pageable);
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
        // 已占用日期 = 档期表里 status != 1 的日期 ∪ 未了结订单占用的婚礼日期。
        // 去重排序后转 yyyy-MM-dd:前端日历靠这份清单在"选的时候"就灰掉不可选的日子。
        // 用 TreeSet 是因为两路来源会重叠(下单时档期表被置 0,同时订单也占着这一天)。
        Set<LocalDate> occupied = new TreeSet<>();
        for (ServiceSchedule s : schedules) {
            if (s.getScheduleDate() != null && (s.getStatus() == null || s.getStatus() != 1)) {
                occupied.add(s.getScheduleDate());
            }
        }
        occupied.addAll(weddingOrderRepository.findOccupiedWeddingDateByServiceIdAndStatusIn(id, OCCUPYING_STATUS));
        List<String> occupiedDates = occupied.stream()
                .map(d -> d.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .toList();

        // 四块一起返回(前三块是老键,前端与测试都在用,不能改名)
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", service);
        result.put("cases", cases);
        result.put("schedules", schedules);
        result.put("occupiedDates", occupiedDates);
        return result;
    }

}
