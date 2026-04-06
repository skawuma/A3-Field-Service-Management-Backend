package com.a3solutions.fsm.dashboard;

import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.dashboard
 * @project A3 Field Service Management Backend
 * @date 4/5/26
 */
public record DashboardAnalytics(
        List<DashboardChartDatum> workOrdersByStatus,
        List<DashboardChartDatum> workOrdersByPriority,
        List<DashboardTrendPoint> completionTrend
) {
}
