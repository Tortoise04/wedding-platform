package com.wedding.service;

import com.wedding.entity.Favorite;

import java.util.List;

// 菜单:收藏能做什么。
public interface FavoriteService {

    // 收藏服务(幂等:已收藏则拦截)
    void addFavorite(Long userId, Long serviceId);

    // 取消收藏
    void removeFavorite(Long userId, Long serviceId);

    // 我的收藏列表
    List<Favorite> listFavorites(Long userId);
}
