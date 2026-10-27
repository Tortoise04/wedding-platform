package com.wedding.controller;

import com.wedding.annotation.RequireLogin;
import com.wedding.common.Result;
import com.wedding.entity.BrowseHistory;
import com.wedding.service.BrowseHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 服务员:用户端 · 浏览记录(路径 /api/browse-history,均 @RequireLogin)。
@RestController
@RequestMapping("/api/browse-history")
@RequiredArgsConstructor
public class BrowseHistoryController {

    private final BrowseHistoryService browseHistoryService;

    // 我的近期浏览记录
    @RequireLogin
    @GetMapping
    public Result<List<BrowseHistory>> list(@RequestAttribute("userId") Long userId) {
        return Result.success(browseHistoryService.listMyHistory(userId));
    }
}
