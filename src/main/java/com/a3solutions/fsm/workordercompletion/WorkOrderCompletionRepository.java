package com.a3solutions.fsm.workordercompletion;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workordercompletion
 * @project A3 Field Service Management Backend
 * @date 3/28/26
 */
public interface WorkOrderCompletionRepository extends JpaRepository<WorkOrderCompletionEntity, Long> {

    Optional<WorkOrderCompletionEntity> findByWorkOrderId(Long workOrderId);

    boolean existsByWorkOrderId(Long workOrderId);
}