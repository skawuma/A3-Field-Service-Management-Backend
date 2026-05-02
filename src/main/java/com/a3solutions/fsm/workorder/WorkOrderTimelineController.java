package com.a3solutions.fsm.workorder;

import com.a3solutions.fsm.exceptions.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3_SOLUTIONS_PROJECT
 * @date 11/26/25
 */
@RestController
@RequestMapping("/api/workorders")
@Tag(
        name = "Work Order Timeline",
        description = "Audit timeline of work-order events for admin and dispatch users."
)
@SecurityRequirement(name = "bearerAuth")
public class WorkOrderTimelineController {

    private final WorkOrderEventService eventService;

    public WorkOrderTimelineController(WorkOrderEventService eventService) {
        this.eventService = eventService;
    }

    @Operation(
            summary = "Get work-order timeline",
            description = "Returns the ordered event history for a work order, including assignment, status changes, notes, attachments, completion, and reopen events."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Timeline returned", content = @Content(array = @ArraySchema(schema = @Schema(implementation = WorkOrderEventDto.class)))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not allowed to view timeline", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}/events")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    public ResponseEntity<List<WorkOrderEventDto>> getTimeline(
            @Parameter(description = "Work order identifier.", example = "42") @PathVariable("id") Long workOrderId
    ) {
        return ResponseEntity.ok(eventService.getTimeline(workOrderId)
                .stream()
                .map(eventService::toDto)
                .toList());
    }
}
