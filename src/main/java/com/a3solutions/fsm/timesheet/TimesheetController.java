package com.a3solutions.fsm.timesheet;

import com.a3solutions.fsm.auth.UserDetailsImpl;
import com.a3solutions.fsm.exceptions.ApiErrorResponse;
import com.a3solutions.fsm.exceptions.ValidationErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.timesheet
 * @project A3 Field Service Management Backend
 * @date 07/03/26
 */
@RestController
@RequestMapping("/api/timesheets")
@Tag(
        name = "Timesheets",
        description = "Weekly technician timesheets, completed-work-order automation, and payroll review workflows."
)
@SecurityRequirement(name = "bearerAuth")
public class TimesheetController {

    private final TimesheetService timesheetService;

    public TimesheetController(TimesheetService timesheetService) {
        this.timesheetService = timesheetService;
    }

    @GetMapping("/my/current")
    @PreAuthorize("hasRole('TECH')")
    @Operation(
            summary = "Get the authenticated technician's current weekly timesheet",
            description = "TECH only. Returns the current Monday-Sunday timesheet for the authenticated technician, creating a DRAFT shell when the week has no timesheet yet."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current weekly timesheet returned", content = @Content(schema = @Schema(implementation = TimesheetResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request could not be processed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller must have the TECH role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Technician profile was not found for the authenticated user", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Timesheet could not be created because of a business rule or duplicate weekly record", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TimesheetResponse> getMyCurrent(Authentication authentication) {
        UserDetailsImpl user = principal(authentication);
        return ResponseEntity.ok(timesheetService.getMyCurrent(user.getId()));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TECH')")
    @Operation(
            summary = "Get the authenticated technician's timesheet for a week",
            description = "TECH only. Normalizes the supplied date to the Monday of that week, then returns or creates the authenticated technician's DRAFT weekly timesheet."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Weekly timesheet returned", content = @Content(schema = @Schema(implementation = TimesheetResponse.class))),
            @ApiResponse(responseCode = "400", description = "weekStart is missing or is not a valid ISO date", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller must have the TECH role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Technician profile was not found for the authenticated user", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Timesheet could not be created because of a business rule or duplicate weekly record", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TimesheetResponse> getMyForWeek(
            @Parameter(description = "Any date in the requested payroll week. The backend normalizes it to Monday.", example = "2026-07-03", required = true)
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStart,
            Authentication authentication
    ) {
        UserDetailsImpl user = principal(authentication);
        return ResponseEntity.ok(timesheetService.getMyForWeek(user.getId(), weekStart));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    @Operation(
            summary = "List technician timesheets",
            description = "ADMIN and DISPATCH only. Returns timesheets for payroll review with optional week, technician, and status filters."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Timesheets returned", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TimesheetResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Filter value is invalid", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller must have ADMIN or DISPATCH role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<TimesheetResponse>> getAll(
            @Parameter(description = "Optional week filter. Any date in the week is accepted and normalized to Monday.", example = "2026-07-03")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStart,
            @Parameter(description = "Optional technician profile id filter.", example = "7")
            @RequestParam(required = false) Long technicianId,
            @Parameter(description = "Optional timesheet lifecycle status filter.", example = "SUBMITTED")
            @RequestParam(required = false) TimesheetStatus status
    ) {
        return ResponseEntity.ok(timesheetService.getAll(weekStart, technicianId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    @Operation(
            summary = "Get a timesheet by identifier",
            description = "ADMIN and DISPATCH can view any timesheet. TECH can view only timesheets owned by their technician profile."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Timesheet returned", content = @Content(schema = @Schema(implementation = TimesheetResponse.class))),
            @ApiResponse(responseCode = "400", description = "Timesheet id is invalid", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not allowed to view this timesheet", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Timesheet not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TimesheetResponse> getById(
            @Parameter(description = "Timesheet identifier.", example = "12")
            @PathVariable Long id,
            Authentication authentication
    ) {
        UserDetailsImpl user = principal(authentication);
        return ResponseEntity.ok(timesheetService.getById(id, user.getId(), user.getRole()));
    }

    @PutMapping("/entries/{entryId}")
    @PreAuthorize("hasRole('TECH')")
    @Operation(
            summary = "Update a timesheet entry",
            description = "TECH only. Updates editable payroll fields on one of the authenticated technician's entries. Entries are editable only while the parent timesheet is DRAFT or REJECTED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Timesheet entry updated", content = @Content(schema = @Schema(implementation = TimesheetEntryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller must own the entry and have TECH role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Timesheet entry or technician profile not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Parent timesheet is locked or the update violates a timesheet business rule", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TimesheetEntryResponse> updateEntry(
            @Parameter(description = "Timesheet entry identifier.", example = "42")
            @PathVariable Long entryId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Editable payroll fields such as mileage, onsite/offsite times, break times, and comments.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = TimesheetEntryUpdateRequest.class))
            )
            @Valid @RequestBody TimesheetEntryUpdateRequest request,
            Authentication authentication
    ) {
        UserDetailsImpl user = principal(authentication);
        return ResponseEntity.ok(timesheetService.updateEntry(entryId, request, user.getId()));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('TECH')")
    @Operation(
            summary = "Submit a weekly timesheet",
            description = "TECH only. Submits the authenticated technician's DRAFT or REJECTED timesheet for payroll review after validating that it has at least one valid work entry and a typed signature."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Timesheet submitted", content = @Content(schema = @Schema(implementation = TimesheetResponse.class))),
            @ApiResponse(responseCode = "400", description = "Signature payload is invalid", content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller must own the timesheet and have TECH role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Timesheet or technician profile not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Timesheet cannot be submitted from its current status or has incomplete entries", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TimesheetResponse> submit(
            @Parameter(description = "Timesheet identifier.", example = "12")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Typed technician signature used to submit the weekly timesheet.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = TimesheetSubmitRequest.class))
            )
            @Valid @RequestBody TimesheetSubmitRequest request,
            Authentication authentication
    ) {
        UserDetailsImpl user = principal(authentication);
        return ResponseEntity.ok(timesheetService.submit(id, request, user.getId()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    @Operation(
            summary = "Approve a submitted timesheet",
            description = "ADMIN and DISPATCH only. Moves a SUBMITTED timesheet to APPROVED, locking it against further technician edits."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Timesheet approved", content = @Content(schema = @Schema(implementation = TimesheetResponse.class))),
            @ApiResponse(responseCode = "400", description = "Timesheet id is invalid", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller must have ADMIN or DISPATCH role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Timesheet not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Only SUBMITTED timesheets can be approved", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TimesheetResponse> approve(
            @Parameter(description = "Timesheet identifier.", example = "12")
            @PathVariable Long id,
            Authentication authentication
    ) {
        UserDetailsImpl user = principal(authentication);
        return ResponseEntity.ok(timesheetService.approve(id, user.getRole()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    @Operation(
            summary = "Reject a submitted timesheet",
            description = "ADMIN and DISPATCH only. Moves a SUBMITTED timesheet to REJECTED so the owning technician can correct payroll details and resubmit."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Timesheet rejected", content = @Content(schema = @Schema(implementation = TimesheetResponse.class))),
            @ApiResponse(responseCode = "400", description = "Timesheet id is invalid", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller must have ADMIN or DISPATCH role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Timesheet not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Only SUBMITTED timesheets can be rejected", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TimesheetResponse> reject(
            @Parameter(description = "Timesheet identifier.", example = "12")
            @PathVariable Long id,
            Authentication authentication
    ) {
        UserDetailsImpl user = principal(authentication);
        return ResponseEntity.ok(timesheetService.reject(id, user.getRole()));
    }

    @GetMapping("/{id}/export/csv")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    @Operation(
            summary = "Export a timesheet as CSV",
            description = "ADMIN and DISPATCH can export any timesheet. TECH can export only their own timesheets. The CSV includes payroll-ready weekly rows and the technician signature text."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSV export returned", content = @Content(mediaType = "text/csv", schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", description = "Timesheet id is invalid", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not allowed to export this timesheet", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Timesheet not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<byte[]> exportCsv(
            @Parameter(description = "Timesheet identifier.", example = "12")
            @PathVariable Long id,
            Authentication authentication
    ) {
        UserDetailsImpl user = principal(authentication);
        byte[] content = timesheetService.exportCsv(id, user.getId(), user.getRole());
        String filename = "a3-fsm-timesheet-" + id + ".csv";
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString()
                )
                .body(content);
    }

    private UserDetailsImpl principal(Authentication authentication) {
        return (UserDetailsImpl) authentication.getPrincipal();
    }
}
