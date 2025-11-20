package com.a3solutions.fsm.dashboard;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.dashboard
 * @project A3 Field Service Management Backend
 * @date 11/19/25
 */
public record DashboardSummary(
        long totalTechnicians,
        long totalWorkOrders,
        long openWorkOrders,
        long inProgressWorkOrders,
        long unassignedWorkOrders,
        long scheduledToday)

{ }
