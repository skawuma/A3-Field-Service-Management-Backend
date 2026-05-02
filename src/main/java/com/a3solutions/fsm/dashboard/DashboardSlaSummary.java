package com.a3solutions.fsm.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.dashboard
 * @project A3 Field Service Management Backend
 * @date 4/7/26
 */
@Schema(description = "SLA counts plus the work orders that are overdue or due today.")
public record DashboardSlaSummary( long overdueCount,
                                   long dueTodayCount,
                                   List<DashboardSlaWorkOrderItem> overdueItems,
                                   List<DashboardSlaWorkOrderItem> dueTodayItems) {
}
