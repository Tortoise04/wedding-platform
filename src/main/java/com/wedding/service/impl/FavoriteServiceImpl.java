package com.wedding.service.impl;

import com.wedding.entity.Favorite;
import com.wedding.exception.BusinessException;
import com.wedding.repository.FavoriteRepository;
import com.wedding.repository.WeddingServiceRepository;
import com.wedding.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 厨师:收藏这道菜(普通 CRUD,无状态流转)。
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final WeddingServiceRepository weddingServiceRepository;

    // 新增收藏:先确认服务存在,再挡重复收藏(幂等)。
    @Transactional
    @Override
    public void addFavorite(Long userId, Long serviceId) {
        weddingServiceRepository.findById(serviceId)
                .orElseThrow(() -> new BusinessException("服务不存在: " + serviceId));

        if (favoriteRepository.existsByUserIdAndServiceId(userId, serviceId)) {
            throw new BusinessException("已收藏该服务");
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setServiceId(serviceId);
        favoriteRepository.save(favorite);
    }

    // 取消收藏:按用户 + 服务直接删除,本来没收藏也当成功(幂等)。
    @Transactional
    @Override
    public void removeFavorite(Long userId, Long serviceId) {
        favoriteRepository.deleteByUserIdAndServiceId(userId, serviceId);
    }

    // 我的收藏列表:按收藏时间倒序。
    @Override
    public List<Favorite> listFavorites(Long userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
