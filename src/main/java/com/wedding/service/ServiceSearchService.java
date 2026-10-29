package com.wedding.service;

import com.wedding.dto.ServiceSearchQuery;
import com.wedding.entity.WeddingService;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.Map;

// 厨师:服务检索这道菜(需求 FR-U-03)。
//
// 覆盖全部检索维度:关键词(FR-U-03-1)· 类型(FR-U-03-2)· 价格区间(FR-U-03-3)· 排序(FR-U-03-5)· 组合与分页(FR-U-03-6)。
// ⚠️ 返回 Page(带总数/总页数);入参只有一个查询对象 ServiceSearchQuery。
public interface ServiceSearchService {

    // 多条件检索(条件全可选,为 null 不参与筛选,条件间是 AND;分页排序由 query 里的 page/size/sort 决定)
    Page<WeddingService> search(ServiceSearchQuery query);
    // 按 id 查服务详情(登录用户会顺带记一条浏览记录,游客 userId 传 null 则不记)
    Map<String, Object> getDetail(Long id, Long userId);
    // 列表卡片的「最近可约」概览:入参是本页的服务 id,一次 group by 查完,返回 { "服务id": {bookableFrom, bookableCount} }。
    // 键用字符串:序列化成 JSON 后对象键本来就是字符串,前端直接 bookable[svc.id] 取。
    // id 集合为空时不发查询,直接返回空表;只统计未来(status=1 且 schedule_date >= 今天)的档期。
    Map<String, Map<String, Object>> bookableSummary(Collection<Long> serviceIds);
    // 门户 · 某服务的公开评价:只取 APPROVED,返回 {count, average, distribution:{5..1}, list(最近 20 条)}。
    // list 每项只含展示字段 + 脱敏后的 userLabel,userId / orderId 不出接口。
    Map<String, Object> publicEvaluations(Long serviceId);
}
