package com.wedding.service.impl;

import com.wedding.entity.BrowseHistory;
import com.wedding.repository.BrowseHistoryRepository;
import com.wedding.service.BrowseHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// 厨师:浏览记录(只读,无状态流转)。
@Service
@RequiredArgsConstructor
public class BrowseHistoryServiceImpl implements BrowseHistoryService {

    private final BrowseHistoryRepository browseHistoryRepository;

    // 我的近期浏览记录,按时间倒序。
    @Override
    public List<BrowseHistory> listMyHistory(Long userId) {
        return browseHistoryRepository.findTop20ByUserIdOrderByBrowseTimeDesc(userId);
    }
}
