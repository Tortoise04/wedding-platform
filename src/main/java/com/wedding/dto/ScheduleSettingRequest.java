package com.wedding.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

// 商家设置时段占用的请求体(FR-M-02-4):一批日期 × 一批时段,每个组合落一条占用记录。
//
// 兼容老形态 {dates, slot, venue}:slots 缺省时按 slot 单值处理。
// 同一个 DTO 也复用作"改单条档期"的请求体(那时只用 slot 与 venue)。
@Data
public class ScheduleSettingRequest {

    // 可预约日期列表(非空)
    private List<LocalDate> dates;

    // 占用时段:整天 / 上午 / 下午 / 晚间(缺省=整天,兼容老前端只传 dates 的写法)
    private String slot;

    // 占用时段多选:与 dates 做笛卡尔积;缺省时回退到上面的 slot 单值
    private List<String> slots;

    // 场地名称(选填)
    private String venue;
}
