package com.a3solutions.fsm.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.dashboard
 * @project A3 Field Service Management Backend
 * @date 11/19/25
 */
@Schema(description = "Top-level dashboard KPI summary.")
public record DashboardSummary(
        long totalTechnicians,
        long totalWorkOrders,
        long openWorkOrders,
        long inProgressWorkOrders,
        long unassignedWorkOrders,
        long scheduledToday,
        long dueTodayWorkOrders,
        long overdueWorkOrders,
        long completedToday,
        long highPriorityOpen,
        long activeAssignedWorkOrders,
        long assignedInProgressWorkOrders
) {
}
