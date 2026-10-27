package com.wedding.repository;

import com.wedding.entity.ServiceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// 冷库:服务档期的存取。
//
// 按方法名生成查询:findByServiceIdOrderByScheduleDateAsc 即 WHERE service_id = ? ORDER BY schedule_date ASC。
public interface ServiceScheduleRepository extends JpaRepository<ServiceSchedule, Long> {

    // 某服务的档期,按日期升序(前端从早到晚展示)
    List<ServiceSchedule> findByServiceIdOrderByScheduleDateAsc(Long serviceId);

    // 该服务某天是否已设过档期(写入前查重,避免同一日期出现多条)
    boolean existsByServiceIdAndScheduleDate(Long serviceId, LocalDate scheduleDate);

    // 取某服务某天的档期那一行(下单时置 0、取消时置回 1 都要先拿到它)
    Optional<ServiceSchedule> findByServiceIdAndScheduleDate(Long serviceId, LocalDate scheduleDate);
}
