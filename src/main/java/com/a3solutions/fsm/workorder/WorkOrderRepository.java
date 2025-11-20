package com.a3solutions.fsm.workorder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
public interface WorkOrderRepository extends JpaRepository<WorkOrderEntity, Long> {

    long countByStatus(WorkOrderStatus status);
    long countByAssignedTechIdIsNull();
    long countByScheduledDate(LocalDate date);

}