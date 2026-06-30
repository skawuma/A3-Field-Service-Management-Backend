package com.a3solutions.fsm.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Completed-work SLA performance for one technician.")
public record DashboardTechnicianSlaPerformance(
        Long technicianId,
        String technicianName,
        long completedCount,
        long withinSlaCount,
        long breachedCount,
        double compliancePercent,
        long averageCompletionMinutes
) {
}
