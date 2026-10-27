package com.wedding.controller;

import com.wedding.annotation.RequireLogin;
import com.wedding.common.Result;
import com.wedding.entity.Favorite;
import com.wedding.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 服务员:用户端 · 我的收藏(路径 /api/favorites,全部 @RequireLogin,只操作自己的收藏)。
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    // 我的收藏列表(仅当前登录用户自己的)
    @RequireLogin
    @GetMapping
    public Result<List<Favorite>> list(@RequestAttribute("userId") Long userId) {
        return Result.success(favoriteService.listFavorites(userId));
    }

    // 收藏服务
    @RequireLogin
    @PostMapping
    public Result<Void> add(@RequestParam Long serviceId,
                            @RequestAttribute("userId") Long userId) {
        favoriteService.addFavorite(userId, serviceId);
        return Result.success(null);
    }

    // 取消收藏(按 serviceId)
    @RequireLogin
    @DeleteMapping("/{serviceId}")
    public Result<Void> remove(@PathVariable Long serviceId,
                               @RequestAttribute("userId") Long userId) {
        favoriteService.removeFavorite(userId, serviceId);
        return Result.success(null);
    }
}
