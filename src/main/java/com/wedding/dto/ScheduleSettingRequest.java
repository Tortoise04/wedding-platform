package com.wedding.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

// 商家设置档期的请求体(FR-M-02-4):一批可预约日期,每个日期落一条档期。
@Data
public class ScheduleSettingRequest {

    // 可预约日期列表(非空)
    private List<LocalDate> dates;
}
