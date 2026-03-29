package com.a3solutions.fsm.workorder;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3_SOLUTIONS_PROJECT
 * @date 11/28/25
 */
public record WorkOrderUpdateRequest(
//        String clientName,
//        String address,
//        String description,
//        String priority,
//        String status,
//        String scheduledDate,
//        Long assignedTechId

        String clientName,
        String address,
        String description,
        String priority,
        String status,
        String scheduledDate,
        Long assignedTechId
) {
}
