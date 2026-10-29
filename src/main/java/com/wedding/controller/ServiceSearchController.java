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
import java.util.List;
import java.util.Map;

// 服务员:门户端 · 服务检索(需求 FR-U-01)。
//
// 不贴权限注解 = 游客可访问(检索的是公开内容)。
// ⚠️ 形参直接写 ServiceSearchQuery,Spring MVC 会自动把同名 URL 参数塞进去(参数对象绑定)。
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceSearchController {

    // 与其他 Controller 一致,注入 ServiceSearchService 接口(「最近可约」的批量方法已声明在接口上)。
    private final ServiceSearchService serviceSearchService;

    // 门户 · 服务搜索。
    //   参数全可选:keyword / categoryId / priceMin / priceMax / bookableDate / sort(newest/priceAsc/priceDesc/hot,默认 newest) / page / size(默认每页 10 条)。
    //   返回 {total, totalPages, page, size, list, bookable}。
    @GetMapping("/search")
    public Result<Map<String, Object>> search(ServiceSearchQuery query) {

        Page<WeddingService> page = serviceSearchService.search(query);
        List<WeddingService> list = page.getContent();

        // ⚠️ 不直接把 Page 返回前端:PageImpl 的 JSON 序列化不稳定(Spring Boot 会警告),前端也只需这几个字段,故手工装 Map。
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", page.getTotalElements());   // 命中总数
        data.put("totalPages", page.getTotalPages()); // 总页数
        data.put("page", page.getNumber());           // 当前页(从 0 起)
        data.put("size", page.getSize());             // 每页条数
        data.put("list", list);                       // 当页数据
        // 每张卡片的「最近可约」:bookable[服务id] = {bookableFrom, bookableCount}。
        // 只在当页 id 上查一次(服务层一条 group by),不随卡片数增加查询次数;键缺失 = 该服务没有未来可约档期。
        data.put("bookable", serviceSearchService.bookableSummary(
                list.stream().map(WeddingService::getId).toList()));

        return Result.success(data);
    }
    // 门户 · 服务详情(按 id;服务不存在或未审核通过则报错)。
    // userId 可选:公开接口,登录了才拿得到 userId(游客为 null,不记浏览)。
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id,
                                              @RequestAttribute(value = "userId", required = false) Long userId) {
        return Result.success(serviceSearchService.getDetail(id, userId));
    }

    // 门户 · 某服务的公开评价(游客可访问,和上面的详情一样不贴权限注解)。
    //   只放已通过审核的:返回 {count, average, distribution:{5..1}, list},list 只给展示字段与脱敏标签。
    @GetMapping("/{id}/evaluations")
    public Result<Map<String, Object>> evaluations(@PathVariable Long id) {
        return Result.success(serviceSearchService.publicEvaluations(id));
    }
}
