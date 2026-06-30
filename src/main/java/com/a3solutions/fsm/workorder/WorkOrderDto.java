package com.a3solutions.fsm.workorder;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
@Schema(description = "Work order response model.")
public record WorkOrderDto(
        @Schema(description = "Work order identifier.", example = "42")
        Long id,
        @Schema(description = "Customer or client name.", example = "Acme Manufacturing")
        String clientName,
        @Schema(description = "Service location address.", example = "123 Main St, Chicago, IL")
        String address,
        @Schema(description = "Work description or issue summary.", example = "Generator panel fault on startup")
        String description,
        @Schema(description = "Current workflow status.", example = "IN_PROGRESS")
        WorkOrderStatus status,
        @Schema(description = "Assigned technician id.", example = "3")
        Long assignedTechId,
        @Schema(description = "Assigned technician display name.", example = "Deborah Katimbo")
        String assignedTechnicianName,
        @Schema(description = "Scheduled service date.", example = "2026-04-25")
        LocalDate scheduledDate,
        @Schema(description = "Priority label.", example = "HIGH")
        String priority,
        @Schema(description = "Stored signature URL after completion.")
        String signatureUrl,
        @Schema(description = "Technician completion notes.")
        String completionNotes,
        @Schema(description = "Completion timestamp.") Instant completedAt,
        @Schema(description = "Creation timestamp; starts the time-to-assign and resolution clocks.") Instant createdAt,
        @Schema(description = "Technician assignment timestamp.") Instant assignedAt,
        @Schema(description = "Technician acceptance timestamp, when used by policy.") Instant acceptedAt,
        @Schema(description = "Travel start timestamp; starts the execution SLA in the Sprint 11 policy.") Instant enRouteAt,
        @Schema(description = "Onsite arrival timestamp.") Instant arrivedAt,
        @Schema(description = "Actual work start timestamp.") Instant workStartedAt,
        @Schema(description = "Timestamp selected by policy as the execution SLA anchor.") Instant slaClockStartedAt,
        @Schema(description = "Execution SLA deadline.") Instant slaDueAt,
        @Schema(description = "Whether the execution SLA was breached.") Boolean slaBreached,
        @Schema(description = "Configured execution SLA duration in minutes.") Integer slaDurationMinutes,
        @Schema(description = "Actual execution duration in minutes.") Integer actualCompletionMinutes,
        @Schema(description = "Minutes beyond the execution SLA; zero when met.") Integer breachMinutes,
        @Schema(description = "Dispatch clock: minutes from creation to assignment.") Long timeToAssignMinutes,
        @Schema(description = "Response clock: minutes from assignment to SLA/travel start.") Long timeToStartMinutes,
        @Schema(description = "Resolution clock: minutes from creation to completion.") Long resolutionMinutes
) {
}
