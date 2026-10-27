package com.wedding.service;

import com.wedding.entity.BrowseHistory;

import java.util.List;

// 厨师:浏览记录这道菜(需求 FR-U-06-3)。
public interface BrowseHistoryService {

    // 我的近期浏览记录(按浏览时间倒序,只取最近若干条)
    List<BrowseHistory> listMyHistory(Long userId);
}
