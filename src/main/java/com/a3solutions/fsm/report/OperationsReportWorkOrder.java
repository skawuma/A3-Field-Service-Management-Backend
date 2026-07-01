package com.a3solutions.fsm.report;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "Work-order detail row included in an operational report.")
public record OperationsReportWorkOrder(
        Long id,
        String reference,
        String clientName,
        String description,
        String status,
        String priority,
        LocalDate scheduledDate,
        String technicianName,
        String slaOutcome,
        Long resolutionMinutes,
        Instant createdAt,
        Instant completedAt
) {
}
