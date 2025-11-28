package com.a3solutions.fsm.workorder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3_SOLUTIONS_PROJECT
 * @date 11/26/25
 */
public interface WorkOrderEventRepository extends JpaRepository<WorkOrderEventEntity, Long> {

    List<WorkOrderEventEntity> findByWorkOrderIdOrderByCreatedAtAsc(Long workOrderId);
}