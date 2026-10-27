package com.wedding.repository;

import com.wedding.entity.ServiceCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 冷库:服务案例素材的存取。
//
// 按方法名生成查询:findByServiceIdOrderBySortOrderAsc 即 WHERE service_id = ? ORDER BY sort_order ASC。
public interface ServiceCaseRepository extends JpaRepository<ServiceCase, Long> {

    // 某服务的案例素材,按 sort_order 升序(前端展示顺序)
    List<ServiceCase> findByServiceIdOrderBySortOrderAsc(Long serviceId);
}
