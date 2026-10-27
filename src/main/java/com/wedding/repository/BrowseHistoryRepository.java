package com.wedding.repository;

import com.wedding.entity.BrowseHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

// 冷库:浏览记录的存取。
public interface BrowseHistoryRepository extends JpaRepository<BrowseHistory, Long> {

    // 某用户最近的浏览记录,按浏览时间倒序(只取最近 20 条,避免列表越拉越长)
    List<BrowseHistory> findTop20ByUserIdOrderByBrowseTimeDesc(Long userId);

    // 统计:近7天浏览量趋势(本店),按天分组(join t_service 归属本店)。返回每行 [日期字符串, 浏览量]。
    @Query(value = "SELECT DATE_FORMAT(b.browse_time, '%Y-%m-%d') d, COUNT(*) FROM t_browse_history b " +
            "JOIN t_service s ON s.id = b.service_id WHERE s.merchant_id = :merchantId AND b.browse_time >= :since GROUP BY d",
            nativeQuery = true)
    List<Object[]> countBrowseByDaySinceAndMerchant(@Param("merchantId") Long merchantId, @Param("since") LocalDateTime since);
}
