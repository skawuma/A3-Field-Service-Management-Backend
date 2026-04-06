package com.a3solutions.fsm.workorder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
public interface WorkOrderRepository extends JpaRepository<WorkOrderEntity, Long>, JpaSpecificationExecutor<WorkOrderEntity> {

    long countByStatus(WorkOrderStatus status);
    long countByAssignedTechIdIsNull();
    long countByScheduledDate(LocalDate date);
    long countByStatusAndCompletedAtBetween(WorkOrderStatus status, Instant startInclusive, Instant endExclusive);
    long countByStatusAndPriorityIn(WorkOrderStatus status, Collection<String> priorities);

}
