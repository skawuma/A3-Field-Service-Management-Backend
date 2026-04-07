package com.a3solutions.fsm.dashboard;

import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.dashboard
 * @project A3 Field Service Management Backend
 * @date 4/7/26
 */
public record DashboardSlaSummary( long overdueCount,
                                   long dueTodayCount,
                                   List<DashboardSlaWorkOrderItem> overdueItems,
                                   List<DashboardSlaWorkOrderItem> dueTodayItems) {
}
