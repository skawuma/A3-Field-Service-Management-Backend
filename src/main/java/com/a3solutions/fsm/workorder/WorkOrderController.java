package com.a3solutions.fsm.workorder;

import com.a3solutions.fsm.auth.UserDetailsImpl;
import com.a3solutions.fsm.common.PageResponse;
import com.a3solutions.fsm.security.JwtService;
import com.a3solutions.fsm.security.Role;
import com.a3solutions.fsm.workordercompletion.WorkOrderCompletionRequest;
import com.a3solutions.fsm.workordercompletion.WorkOrderCompletionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */

@RestController
@RequestMapping("/api/workorders")
@Tag(
        name = "Work Orders",
        description = "Work order lifecycle, technician execution, completion reporting, and recovery actions."
)
@SecurityRequirement(name = "bearerAuth")
public class WorkOrderController {

    private final WorkOrderService service;
   private final JwtService jwtService;
    public WorkOrderController(WorkOrderService service, JwtService jwtService) {
        this.service = service;
        this.jwtService = jwtService;
    }

    // =====================================================================
    // GET PAGE
    // =====================================================================
    @Operation(
            summary = "List work orders",
            description = "Returns a paged work order list. TECH users are automatically scoped to their own assigned work orders."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work orders returned", content = @Content(schema = @Schema(implementation = WorkOrderPageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Caller does not have permission")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<PageResponse<WorkOrderDto>> getPage(
            @Parameter(description = "Zero-based page index", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Free-text search over customer, address, description, or status", example = "generator") @RequestParam(required = false) String search,
            @Parameter(description = "Priority filter", example = "HIGH") @RequestParam(required = false) String priority,
            @Parameter(description = "Status filter", example = "OPEN") @RequestParam(required = false) String status,
            @Parameter(description = "Sort field and direction", example = "id,desc") @RequestParam(defaultValue = "id,desc") String sort,
            @Parameter(description = "Technician filter. Ignored for TECH users because they are automatically scoped.", example = "3") @RequestParam(required = false) Long technicianId,
            Authentication auth
    ) {

        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        Role role = user.getRole();

        //🔥 TECH: map userId -> technicianId
        if (role == Role.TECH) {
            technicianId = service.findTechnicianIdForUser(user.getId());
        }

        return ResponseEntity.ok(
                service.getPage(page, size, search, priority, status, sort, technicianId)
        );
    }

    // =====================================================================
    // GET ONE — TECH ONLY IF ASSIGNED
    // =====================================================================
//    @GetMapping("/{id}")
//    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
//    public ResponseEntity<?> getOne(
//            @PathVariable Long id,
//            Authentication auth
//    ) {
//        var dto = service.getById(id);
//
//        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
//        Role role = user.getRole();
//
//        if (role == Role.TECH) {
//            if (dto.assignedTechId() == null || !dto.assignedTechId().equals(user.getId())) {
//                return ResponseEntity.status(403).body("Not authorized to view this work order.");
//            }
//        }
//
//        return ResponseEntity.ok(dto);
//    }

    @Operation(
            summary = "Get a single work order",
            description = "Returns one work order. TECH users may only view work orders currently assigned to them."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work order returned", content = @Content(schema = @Schema(implementation = WorkOrderDto.class))),
            @ApiResponse(responseCode = "403", description = "TECH user is not assigned to this work order"),
            @ApiResponse(responseCode = "404", description = "Work order not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<?> getOne(
            @Parameter(description = "Work order identifier", example = "42") @PathVariable Long id,
            Authentication auth
    ) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        Role role = user.getRole();

        if (role == Role.TECH) {
            boolean allowed = service.canTechAccessWorkOrder(id, user.getId());
            if (!allowed) {
                return ResponseEntity.status(403)
                        .body("Not authorized to view this work order.");
            }
        }

        var dto = service.getById(id);
        return ResponseEntity.ok(dto);
    }


    // =====================================================================
    // CREATE
    // =====================================================================
    @Operation(
            summary = "Create a work order",
            description = "Creates a new work order. Intended for ADMIN and DISPATCH users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work order created", content = @Content(schema = @Schema(implementation = WorkOrderDto.class))),
            @ApiResponse(responseCode = "400", description = "Request payload is invalid"),
            @ApiResponse(responseCode = "403", description = "Caller does not have permission")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    public ResponseEntity<WorkOrderDto> create(@RequestBody WorkOrderCreateRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    // =====================================================================
    // ASSIGN TECHNICIAN
    // =====================================================================
    @Operation(
            summary = "Assign a technician",
            description = "Assigns a technician to a work order. When the work order is OPEN or ASSIGNED, the status is normalized to ASSIGNED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Technician assigned", content = @Content(schema = @Schema(implementation = WorkOrderDto.class))),
            @ApiResponse(responseCode = "400", description = "Assignment request is invalid"),
            @ApiResponse(responseCode = "404", description = "Work order or technician not found")
    })
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    public ResponseEntity<WorkOrderDto> assignTechnician(
            @Parameter(description = "Work order identifier", example = "42") @PathVariable Long id,
            @RequestBody AssignTechnicianRequest request,
            HttpServletRequest httpReq
    ) {
        // Extract username/email from JWT
        String authHeader = httpReq.getHeader("Authorization");
        String token = authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : null;

//        String actor = jwtService.extractUsername(token);

        // User's email from Security Context
        String actor = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return ResponseEntity.ok(service.assignTechnician(id, request, actor));
    }


    // =====================================================================
    // UPDATE WORK ORDER — TECH LIMITED
    // =====================================================================
    @Operation(
            summary = "Update a work order",
            description = "ADMIN and DISPATCH can perform full updates. TECH users may only update work orders assigned to them, using the technician-safe update path."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work order updated", content = @Content(schema = @Schema(implementation = WorkOrderDto.class))),
            @ApiResponse(responseCode = "403", description = "TECH user is not allowed to update this work order"),
            @ApiResponse(responseCode = "404", description = "Work order not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<?> update(
            @Parameter(description = "Work order identifier", example = "42") @PathVariable Long id,
            @RequestBody WorkOrderCreateRequest req,
            Authentication auth
    ) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        Role role = user.getRole();

        if (role == Role.TECH) {

            if (!service.canTechAccessWorkOrder(id, user.getId())) {
                return ResponseEntity.status(403).body("TECH can only update assigned work orders.");
            }

            return ResponseEntity.ok(service.updateTechByUser(id, req, user.getId()));
        }

        return ResponseEntity.ok(service.updateAdmin(id, req));
    }

    @Operation(
            summary = "Start a work order",
            description = "TECH only. Transitions a work order from OPEN or ASSIGNED to IN_PROGRESS. The caller must be the assigned technician."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work order started", content = @Content(schema = @Schema(implementation = WorkOrderDto.class))),
            @ApiResponse(responseCode = "400", description = "Illegal workflow transition"),
            @ApiResponse(responseCode = "403", description = "TECH user is not assigned to this work order"),
            @ApiResponse(responseCode = "404", description = "Work order not found")
    })
    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('TECH')")
    public ResponseEntity<?> startWorkOrder(
            @Parameter(description = "Work order identifier", example = "42") @PathVariable Long id,
            Authentication auth
    ) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();

        if (!service.canTechAccessWorkOrder(id, user.getId())) {
            return ResponseEntity.status(403)
                    .body("TECH can only start assigned work orders.");
        }

        return ResponseEntity.ok(service.startWorkOrder(id, user.getId()));
    }

    @Operation(
            summary = "Return a work order to OPEN",
            description = "TECH only. Releases an assigned work order back to OPEN so dispatch/admin can reassign it. Not allowed once work is IN_PROGRESS or COMPLETED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work order returned to OPEN", content = @Content(schema = @Schema(implementation = WorkOrderDto.class))),
            @ApiResponse(responseCode = "400", description = "Illegal workflow transition"),
            @ApiResponse(responseCode = "403", description = "TECH user is not assigned to this work order"),
            @ApiResponse(responseCode = "404", description = "Work order not found")
    })
    @PostMapping("/{id}/return-to-open")
    @PreAuthorize("hasRole('TECH')")
    public ResponseEntity<?> returnWorkOrderToOpen(
            @Parameter(description = "Work order identifier", example = "42") @PathVariable Long id,
            @RequestBody(required = false) ReturnToOpenRequest request,
            Authentication auth
    ) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();

        if (!service.canTechAccessWorkOrder(id, user.getId())) {
            return ResponseEntity.status(403)
                    .body("TECH can only release assigned work orders.");
        }

        String reason = request != null ? request.reason() : null;
        return ResponseEntity.ok(service.returnWorkOrderToOpen(id, user.getId(), reason));
    }

    @Operation(
            summary = "Complete a work order",
            description = "TECH only. Completes an IN_PROGRESS work order. Requires the assigned technician, a signature, and an existing structured completion report."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work order completed", content = @Content(schema = @Schema(implementation = WorkOrderDto.class))),
            @ApiResponse(responseCode = "400", description = "Completion request violates workflow rules"),
            @ApiResponse(responseCode = "403", description = "TECH user is not assigned to this work order"),
            @ApiResponse(responseCode = "404", description = "Work order not found")
    })
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('TECH')")
    public ResponseEntity<?> completeWorkOrder(
            @Parameter(description = "Work order identifier", example = "42") @PathVariable Long id,
            @RequestBody CompleteWorkOrderRequest req,
            Authentication auth
    ) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();

        if (!service.canTechAccessWorkOrder(id, user.getId())) {
            return ResponseEntity.status(403)
                    .body("TECH can only complete assigned work orders.");
        }

        return ResponseEntity.ok(
                service.completeWorkOrder(id, req, user.getId())
        );
    }

    @Operation(
            summary = "Submit a structured completion report",
            description = "TECH only. Stores the field-service report required before final sign-off. Only the assigned technician may submit it."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Completion report saved", content = @Content(schema = @Schema(implementation = WorkOrderCompletionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Report request is invalid or duplicate"),
            @ApiResponse(responseCode = "403", description = "TECH user is not assigned to this work order"),
            @ApiResponse(responseCode = "404", description = "Work order not found")
    })
    @PostMapping("/{id}/completion-report")
    @PreAuthorize("hasRole('TECH')")
    public ResponseEntity<?> submitCompletionReport(
            @Parameter(description = "Work order identifier", example = "42") @PathVariable Long id,
            @RequestBody WorkOrderCompletionRequest request,
            Authentication auth
    ) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();

        if (!service.canTechAccessWorkOrder(id, user.getId())) {
            return ResponseEntity.status(403)
                    .body("TECH can only submit reports for assigned work orders.");
        }

        return ResponseEntity.ok(
                service.submitStructuredCompletionReport(id, request, user.getId())
        );
    }

    @Operation(
            summary = "Get a structured completion report",
            description = "Returns the structured field report for a work order. TECH users may only view reports for their assigned work orders."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Completion report returned", content = @Content(schema = @Schema(implementation = WorkOrderCompletionResponse.class))),
            @ApiResponse(responseCode = "403", description = "TECH user is not assigned to this work order"),
            @ApiResponse(responseCode = "404", description = "Completion report or work order not found")
    })
    @GetMapping("/{id}/completion-report")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<?> getCompletionReport(
            @Parameter(description = "Work order identifier", example = "42") @PathVariable Long id,
            Authentication auth
    ) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        Role role = user.getRole();

        if (role == Role.TECH && !service.canTechAccessWorkOrder(id, user.getId())) {
            return ResponseEntity.status(403)
                    .body("TECH can only view reports for assigned work orders.");
        }

        return ResponseEntity.ok(
                service.getCompletionByWorkOrderId(id)
        );
    }

    @Operation(
            summary = "Get a work-order signature",
            description = "Returns the stored signature asset for a completed work order. TECH users may only access signatures for their assigned work orders."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Signature returned"),
            @ApiResponse(responseCode = "403", description = "TECH user is not assigned to this work order"),
            @ApiResponse(responseCode = "404", description = "Signature or work order not found")
    })
    @GetMapping("/{id}/signature")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<?> getSignature(
            @Parameter(description = "Work order identifier", example = "42") @PathVariable Long id,
            Authentication auth
    ) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        Role role = user.getRole();

        if (role == Role.TECH && !service.canTechAccessWorkOrder(id, user.getId())) {
            return ResponseEntity.status(403).body("TECH can only view signature for assigned work orders!!");
        }

        return service.getSignature(id);
    }

    @Operation(
            summary = "Reopen a completed work order",
            description = "ADMIN or DISPATCH only. Reopens a COMPLETED work order back to OPEN, clearing completion artifacts so it can be re-dispatched."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work order reopened", content = @Content(schema = @Schema(implementation = WorkOrderDto.class))),
            @ApiResponse(responseCode = "400", description = "Only COMPLETED work orders may be reopened"),
            @ApiResponse(responseCode = "404", description = "Work order not found")
    })
    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    public ResponseEntity<WorkOrderDto> reopenWorkOrder(
            @Parameter(description = "Work order identifier", example = "42") @PathVariable Long id,
            @RequestBody(required = false) ReopenWorkOrderRequest request
    ) {
        String reason = request != null ? request.reason() : null;
        return ResponseEntity.ok(service.reopenWorkOrder(id, reason));
    }

}
