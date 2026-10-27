package com.wedding.common;

import org.springframework.data.domain.Page;

import java.util.LinkedHashMap;
import java.util.Map;

// 分页返回体:把 Spring Data 的 Page 统一装成 {list, total, totalPages, page, size}。
// 与 /api/services/search、/api/merchant/services 的分页结构保持一致。
public final class PageResult {

    private PageResult() {
    }

    public static Map<String, Object> of(Page<?> p) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", p.getContent());
        data.put("total", p.getTotalElements());
        data.put("totalPages", p.getTotalPages());
        data.put("page", p.getNumber());
        data.put("size", p.getSize());
        return data;
    }
}
