package com.a3solutions.fsm.dashboard;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.dashboard
 * @project A3 Field Service Management Backend
 * @date 4/7/26
 */
public interface DashboardTechnicianWorkloadProjection {

    Long getTechnicianId();

    String getTechnicianName();

    long getTotalAssignedWorkOrders();

    long getOpenAssignedWorkOrders();

    long getInProgressAssignedWorkOrders();

    long getDueTodayAssignedWorkOrders();

    long getOverdueAssignedWorkOrders();
}
