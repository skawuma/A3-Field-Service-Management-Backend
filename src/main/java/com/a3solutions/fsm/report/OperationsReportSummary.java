package com.a3solutions.fsm.report;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Top-level operational performance metrics for the selected report period.")
public record OperationsReportSummary(
        long totalWorkOrders,
        long activeWorkOrders,
        long completedWorkOrders,
        long cancelledWorkOrders,
        long breachedWorkOrders,
        long completedWithinSla,
        double slaCompliancePercent,
        Long averageTimeToAssignMinutes,
        Long averageTimeToStartMinutes,
        Long averageResolutionMinutes
) {
}
