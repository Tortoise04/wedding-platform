package com.wedding.controller;

import com.wedding.common.Result;
import com.wedding.dto.ServiceSearchQuery;
import com.wedding.entity.WeddingService;
import com.wedding.service.ServiceSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

// 服务员:门户端 · 服务检索(需求 FR-U-01)。
//
// 不贴权限注解 = 游客可访问(检索的是公开内容)。
// ⚠️ 形参直接写 ServiceSearchQuery,Spring MVC 会自动把同名 URL 参数塞进去(参数对象绑定)。
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceSearchController {

    private final ServiceSearchService serviceSearchService;

    // 门户 · 服务搜索。
    //   参数全可选:keyword / categoryId / priceMin / priceMax / sort(newest/priceAsc/priceDesc/hot,默认 newest) / page / size(默认每页 10 条)。
    //   返回 {total, totalPages, page, size, list}。
    @GetMapping("/search")
    public Result<Map<String, Object>> search(ServiceSearchQuery query) {

        Page<WeddingService> page = serviceSearchService.search(query);

        // ⚠️ 不直接把 Page 返回前端:PageImpl 的 JSON 序列化不稳定(Spring Boot 会警告),前端也只需这几个字段,故手工装 Map。
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", page.getTotalElements());   // 命中总数
        data.put("totalPages", page.getTotalPages()); // 总页数
        data.put("page", page.getNumber());           // 当前页(从 0 起)
        data.put("size", page.getSize());             // 每页条数
        data.put("list", page.getContent());          // 当页数据

        return Result.success(data);
    }
    // 门户 · 服务详情(按 id;服务不存在或未审核通过则报错)。
    // userId 可选:公开接口,登录了才拿得到 userId(游客为 null,不记浏览)。
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id,
                                              @RequestAttribute(value = "userId", required = false) Long userId) {
        return Result.success(serviceSearchService.getDetail(id, userId));
    }
}
