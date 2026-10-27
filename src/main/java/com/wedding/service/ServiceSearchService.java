package com.wedding.service;

import com.wedding.dto.ServiceSearchQuery;
import com.wedding.entity.WeddingService;
import org.springframework.data.domain.Page;

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
}
