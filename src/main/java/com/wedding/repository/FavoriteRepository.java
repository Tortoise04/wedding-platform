package com.wedding.repository;

import com.wedding.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 冷库:收藏的存取。
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // 我的收藏列表,按收藏时间倒序
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 是否已收藏(幂等判断)
    boolean existsByUserIdAndServiceId(Long userId, Long serviceId);

    // 查某条收藏记录(可空)
    Optional<Favorite> findByUserIdAndServiceId(Long userId, Long serviceId);

    // 取消收藏:删掉该用户对该服务的收藏记录
    void deleteByUserIdAndServiceId(Long userId, Long serviceId);
}
