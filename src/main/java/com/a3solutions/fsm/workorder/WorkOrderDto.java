package com.a3solutions.fsm.workorder;

import java.time.LocalDate;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
public record WorkOrderDto(
        Long id,
        String clientName,
        String address,
        String description,
        WorkOrderStatus status,
        Long assignedTechId,
        LocalDate scheduledDate,
        String priority
) { }
