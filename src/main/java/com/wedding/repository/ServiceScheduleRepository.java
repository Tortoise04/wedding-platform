package com.wedding.repository;

import com.wedding.entity.ServiceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

// 冷库:服务档期的存取。
//
// 按方法名生成查询:findByServiceIdOrderByScheduleDateAsc 即 WHERE service_id = ? ORDER BY schedule_date ASC。
public interface ServiceScheduleRepository extends JpaRepository<ServiceSchedule, Long> {

    // 某服务的档期,按日期升序(前端从早到晚展示)
    List<ServiceSchedule> findByServiceIdOrderByScheduleDateAsc(Long serviceId);

    // 该服务某天是否已设过档期(写入前查重,避免同一日期出现多条)
    boolean existsByServiceIdAndScheduleDate(Long serviceId, LocalDate scheduleDate);

    // 该服务某天某时段是否已设过档期 —— 时段化之后同一天可以"上午"和"下午"两条并存
    boolean existsByServiceIdAndScheduleDateAndSlot(Long serviceId, LocalDate scheduleDate, String slot);

    // 该服务是否维护过档期 —— 维护过的服务只允许约"有可约时段"的日期;一条都没维护的按天报价,不拦
    boolean existsByServiceId(Long serviceId);

    // 取某服务某天的档期(下单时置 0、取消时置回 1 都要先拿到它)。
    // 时段化之后同一天可能有多条(上午/下午),所以返回 List —— 再用 Optional 接会抛非唯一结果。
    List<ServiceSchedule> findByServiceIdAndScheduleDateOrderBySlotAsc(Long serviceId, LocalDate scheduleDate);
}
