package com.a3solutions.fsm.dashboard;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.dashboard
 * @project A3 Field Service Management Backend
 * @date 4/7/26
 */
public record DashboardTechnicianWorkloadItem(
        Long technicianId,
        String technicianName,
        long totalAssignedWorkOrders,
        long openAssignedWorkOrders,
        long inProgressAssignedWorkOrders,
        long dueTodayAssignedWorkOrders,
        long overdueAssignedWorkOrders
) {
}
