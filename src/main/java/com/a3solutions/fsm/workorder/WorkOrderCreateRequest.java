package com.a3solutions.fsm.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
@Schema(description = "Create or update payload for a work order.")
public record WorkOrderCreateRequest(
        @Schema(description = "Customer or client name.", example = "Acme Manufacturing")
        @NotBlank String clientName,
        @Schema(description = "Service location address.", example = "123 Main St, Chicago, IL")
        @NotBlank String address,
        @Schema(description = "Short description of the reported issue or requested work.", example = "Generator panel fault on startup")
        String description,
        @Schema(description = "Optional assigned technician id.", example = "3")
        Long assignedTechId,
        @Schema(description = "Scheduled service date.", example = "2026-04-25")
        LocalDate scheduledDate,
        @Schema(description = "Priority label.", example = "HIGH")
        String priority,
        @Schema(description = "Requested work order status. Usually managed by workflow actions.", example = "OPEN")
        WorkOrderStatus status
) {
}
