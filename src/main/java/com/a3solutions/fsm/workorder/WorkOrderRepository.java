package com.a3solutions.fsm.workorder;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
public interface WorkOrderRepository extends JpaRepository<WorkOrderEntity, Long> {
}