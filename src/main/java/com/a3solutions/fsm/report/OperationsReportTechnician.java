package com.a3solutions.fsm.report;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Technician completion and SLA performance for the report period.")
public record OperationsReportTechnician(
        Long technicianId,
        String technicianName,
        long completedCount,
        long withinSlaCount,
        long breachedCount,
        double compliancePercent,
        Long averageCompletionMinutes
) {
}
