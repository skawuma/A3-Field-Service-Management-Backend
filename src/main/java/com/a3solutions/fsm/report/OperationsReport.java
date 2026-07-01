package com.a3solutions.fsm.report;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Sprint 12 operational report with chart data, SLA intelligence, and export-ready rows.")
public record OperationsReport(
        Instant generatedAt,
        LocalDate periodStart,
        LocalDate periodEnd,
        String scope,
        OperationsReportSummary summary,
        List<OperationsReportBucket> workOrdersByStatus,
        List<OperationsReportBucket> workOrdersByPriority,
        List<OperationsReportTechnician> technicianPerformance,
        List<OperationsReportWorkOrder> workOrders
) {
}
