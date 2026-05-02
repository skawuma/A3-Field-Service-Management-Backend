package com.a3solutions.fsm.dashboard;

import com.a3solutions.fsm.workorder.WorkOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.dashboard
 * @project A3 Field Service Management Backend
 * @date 4/7/26
 */
@Schema(description = "Work order included in the dashboard SLA summary.")
public record DashboardSlaWorkOrderItem(
                                                Long workOrderId,
                                                String workOrderRef,
                                                String title,
                                                String customerName,
                                                LocalDate scheduledDate,
                                                WorkOrderStatus status,
                                                String priority,
                                                Long assignedTechId,
                                                String assignedTechName,
                                                long daysLate
)
{
}
