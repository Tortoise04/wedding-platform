package com.wedding.service;

import java.util.Map;

// 厨师:数据统计这道菜(商家端 FR-M-05 / 管理端 FR-A-04-4~7)。
//
// 统计是"读多写少"的汇总,统一返回 Map(键值对),前端拿 JSON 直接画图。
public interface StatsService {

    // 商家端:本店铺经营数据(服务/浏览/订单/评价几个维度)
    Map<String, Object> merchantStats(Long userId);

    // 管理端:平台整体数据(用户/商家/服务/订单/热门服务/热门商家)
    Map<String, Object> platformStats();
}
