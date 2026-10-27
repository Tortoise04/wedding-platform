package com.wedding.service.impl;

import com.wedding.entity.Merchant;
import com.wedding.enums.AuditStatus;
import com.wedding.enums.OrderStatus;
import com.wedding.repository.BrowseHistoryRepository;
import com.wedding.repository.EvaluationRepository;
import com.wedding.repository.MerchantRepository;
import com.wedding.repository.UserRepository;
import com.wedding.repository.WeddingOrderRepository;
import com.wedding.repository.WeddingServiceRepository;
import com.wedding.service.MerchantService;
import com.wedding.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 厨师:数据统计。汇总查询全部走仓储的 count / sum / avg / 分组聚合,不在 Java 里拉全量数据再算。
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final MerchantService merchantService;
    private final MerchantRepository merchantRepository;
    private final WeddingServiceRepository weddingServiceRepository;
    private final WeddingOrderRepository weddingOrderRepository;
    private final EvaluationRepository evaluationRepository;
    private final UserRepository userRepository;
    private final BrowseHistoryRepository browseHistoryRepository;

    // 商家端:本店铺经营数据。先解析"我是哪个商家",再按 shopId 汇总各维度。
    @Override
    public Map<String, Object> merchantStats(Long userId) {
        Merchant shop = merchantService.getMyShop(userId);
        Long shopId = shop.getId();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("serviceCount", weddingServiceRepository.countByMerchantId(shopId));
        data.put("onShelfCount", weddingServiceRepository.countByMerchantIdAndShelfStatus(shopId, 1));
        Long viewCount = weddingServiceRepository.sumViewCountByMerchantId(shopId);
        data.put("viewCount", viewCount == null ? 0L : viewCount);
        data.put("orderCount", weddingOrderRepository.countByMerchantId(shopId));
        data.put("pendingOrderCount", weddingOrderRepository.countByMerchantIdAndStatus(shopId, OrderStatus.PENDING));
        data.put("completedOrderCount", weddingOrderRepository.countByMerchantIdAndStatus(shopId, OrderStatus.COMPLETED));
        BigDecimal totalAmount = weddingOrderRepository.sumAmountByMerchantIdAndStatus(shopId, OrderStatus.COMPLETED);
        data.put("totalAmount", totalAmount == null ? BigDecimal.ZERO : totalAmount);
        data.put("evaluationCount", evaluationRepository.countByMerchantIdAndAuditStatus(shopId, AuditStatus.APPROVED));
        Double avgScore = evaluationRepository.avgScoreByMerchantIdAndAuditStatus(shopId, AuditStatus.APPROVED);
        data.put("averageScore", avgScore == null ? 0.0 : avgScore);

        // 本店订单状态分布(逐个状态 count,与平台口径一致)
        Map<String, Long> statusDist = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            statusDist.put(s.name(), weddingOrderRepository.countByMerchantIdAndStatus(shopId, s));
        }
        data.put("orderStatusDistribution", statusDist);

        // 近7天订单量 / 浏览量趋势(只统计本店)
        LocalDateTime since = LocalDate.now().minusDays(6).atStartOfDay();
        data.put("orderTrend", buildDailyTrend(
                weddingOrderRepository.countOrdersByDaySinceAndMerchant(shopId, since),
                browseHistoryRepository.countBrowseByDaySinceAndMerchant(shopId, since),
                7));
        return data;
    }

    // 管理端:平台整体数据。订单状态分布逐个状态 count,热门商家用分组查询取前 5。
    @Override
    public Map<String, Object> platformStats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userCount", userRepository.count());
        data.put("merchantCount", merchantRepository.countByAuditStatus(AuditStatus.APPROVED));
        data.put("pendingMerchantCount", merchantRepository.countByAuditStatus(AuditStatus.PENDING));
        data.put("serviceCount", weddingServiceRepository.count());
        data.put("orderCount", weddingOrderRepository.count());

        // 平台成交总额(已完成订单金额之和)
        BigDecimal totalAmount = weddingOrderRepository.sumAmountByStatus(OrderStatus.COMPLETED);
        data.put("totalAmount", totalAmount == null ? BigDecimal.ZERO : totalAmount);

        Map<String, Long> statusDist = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            statusDist.put(s.name(), weddingOrderRepository.countByStatus(s));
        }
        data.put("orderStatusDistribution", statusDist);

        // 近7天订单量趋势(全站;views 恒 0,与商家端结构保持一致)
        LocalDateTime since = LocalDate.now().minusDays(6).atStartOfDay();
        data.put("orderTrend", buildDailyTrend(
                weddingOrderRepository.countOrdersByDaySince(since), null, 7));

        data.put("topServices", weddingServiceRepository.findTop5ByOrderBySalesCountDesc());

        List<Map<String, Object>> topMerchants = new ArrayList<>();
        for (Object[] row : weddingOrderRepository.findTopMerchantsByOrderCount(PageRequest.of(0, 5))) {
            Long merchantId = (Long) row[0];
            Long orderCount = (Long) row[1];
            Merchant m = merchantRepository.findById(merchantId).orElse(null);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("merchantId", merchantId);
            item.put("shopName", m == null ? null : m.getShopName());
            item.put("orderCount", orderCount);
            topMerchants.add(item);
        }
        data.put("topMerchants", topMerchants);
        return data;
    }

    // 私有工具:把订单按天聚合 + 浏览按天聚合 合并成近 N 天的连续序列(缺的天补 0),保证折线图横轴连续。
    // 只对聚合结果做"补零/对齐",不拉明细,符合"统计只返回聚合结果"的原则。
    private List<Map<String, Object>> buildDailyTrend(List<Object[]> orderRows, List<Object[]> viewRows, int days) {
        Map<String, Long> ordersByDay = groupByDay(orderRows);
        Map<String, Long> viewsByDay = groupByDay(viewRows);
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> series = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            String key = today.minusDays(i).toString();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", key);
            item.put("orders", ordersByDay.getOrDefault(key, 0L));
            item.put("views", viewsByDay.getOrDefault(key, 0L));
            series.add(item);
        }
        return series;
    }

    // 私有工具:把 [日期字符串, 数量] 的聚合行转成 日期→数量 映射。
    private Map<String, Long> groupByDay(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        if (rows == null) {
            return map;
        }
        for (Object[] row : rows) {
            map.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return map;
    }
}
