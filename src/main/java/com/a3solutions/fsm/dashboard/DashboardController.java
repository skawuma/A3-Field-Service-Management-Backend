package com.a3solutions.fsm.dashboard;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.dashboard
 * @project A3 Field Service Management Backend
 * @date 11/19/25
 */
@RestController
@RequestMapping("/api/dashboard")
@Tag(
        name = "Dashboard",
        description = "Operational summary, SLA, analytics, recent activity, and technician workload endpoints."
)
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @Operation(
            summary = "Get dashboard summary",
            description = "Returns top-level dashboard metrics. ADMIN and DISPATCH receive organization-wide metrics, while TECH receives a personal summary."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard summary returned", content = @Content(schema = @Schema(implementation = DashboardSummary.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<DashboardSummary> getSummary() {
        return ResponseEntity.ok(service.getSummary());
    }

    @Operation(
            summary = "Get recent dashboard activity",
            description = "Returns recent work-order activity formatted for dashboard display. TECH users receive role-appropriate activity items."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recent activity returned", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DashboardRecentActivityItem.class)))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/recent-activity")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<List<DashboardRecentActivityItem>> getRecentActivity(
            @Parameter(description = "Maximum number of activity items to return.", example = "8")
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(service.getRecentActivity(limit));
    }

    @Operation(
            summary = "Get dashboard analytics",
            description = "Returns chart-ready datasets for status distribution, priority distribution, and completion trend. Restricted to ADMIN and DISPATCH users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analytics returned", content = @Content(schema = @Schema(implementation = DashboardAnalytics.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not allowed to view analytics", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    public ResponseEntity<DashboardAnalytics> getAnalytics() {
        return ResponseEntity.ok(service.getAnalytics());
    }

    @Operation(
            summary = "Get SLA summary",
            description = "Returns overdue and due-today SLA counts plus the work-order lists behind those counts. TECH users receive only their assigned SLA items."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SLA summary returned", content = @Content(schema = @Schema(implementation = DashboardSlaSummary.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/sla")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<DashboardSlaSummary> getSlaSummary() {
        return ResponseEntity.ok(service.getSlaSummary());
    }

    @Operation(
            summary = "Get technician workload overview",
            description = "Returns workload distribution per technician, including open, in-progress, due-today, and overdue assigned work orders. This powers workload cards and heatmap-style dispatch views."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Technician workload returned", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DashboardTechnicianWorkloadItem.class)))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not allowed to view workload data", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/technician-workload")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    public ResponseEntity<List<DashboardTechnicianWorkloadItem>> getTechnicianWorkload() {
        return ResponseEntity.ok(service.getTechnicianWorkload());
    }
}
