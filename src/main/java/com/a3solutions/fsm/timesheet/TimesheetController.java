package com.a3solutions.fsm.timesheet;

import com.a3solutions.fsm.auth.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Timesheets", description = "Weekly technician timesheets and payroll review workflows.")
@SecurityRequirement(name = "bearerAuth")
public class TimesheetController {

    private final TimesheetService timesheetService;

    public TimesheetController(TimesheetService timesheetService) {
        this.timesheetService = timesheetService;
    }

    @GetMapping("/my/current")
    @PreAuthorize("hasRole('TECH')")
    @Operation(summary = "Get or create the authenticated technician's current weekly timesheet")
    public ResponseEntity<TimesheetResponse> getMyCurrent(Authentication authentication) {
        UserDetailsImpl user = principal(authentication);
        return ResponseEntity.ok(timesheetService.getMyCurrent(user.getId()));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TECH')")
    @Operation(summary = "Get or create the authenticated technician's timesheet for a week")
    public ResponseEntity<TimesheetResponse> getMyForWeek(
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
    @Operation(summary = "List technician timesheets with optional payroll filters")
    public ResponseEntity<List<TimesheetResponse>> getAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStart,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) TimesheetStatus status
    ) {
        return ResponseEntity.ok(timesheetService.getAll(weekStart, technicianId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    @Operation(summary = "Get a timesheet by identifier")
    public ResponseEntity<TimesheetResponse> getById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        UserDetailsImpl user = principal(authentication);
        return ResponseEntity.ok(timesheetService.getById(id, user.getId(), user.getRole()));
    }

    @PutMapping("/entries/{entryId}")
    @PreAuthorize("hasRole('TECH')")
    @Operation(summary = "Update editable payroll fields on a technician's timesheet entry")
    public ResponseEntity<TimesheetEntryResponse> updateEntry(
            @PathVariable Long entryId,
            @Valid @RequestBody TimesheetEntryUpdateRequest request,
            Authentication authentication
    ) {
        UserDetailsImpl user = principal(authentication);
        return ResponseEntity.ok(timesheetService.updateEntry(entryId, request, user.getId()));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('TECH')")
    @Operation(summary = "Submit a technician's weekly timesheet for payroll review")
    public ResponseEntity<TimesheetResponse> submit(
            @PathVariable Long id,
            @Valid @RequestBody TimesheetSubmitRequest request,
            Authentication authentication
    ) {
        UserDetailsImpl user = principal(authentication);
        return ResponseEntity.ok(timesheetService.submit(id, request, user.getId()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    @Operation(summary = "Approve a submitted timesheet")
    public ResponseEntity<TimesheetResponse> approve(
            @PathVariable Long id,
            Authentication authentication
    ) {
        UserDetailsImpl user = principal(authentication);
        return ResponseEntity.ok(timesheetService.approve(id, user.getRole()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    @Operation(summary = "Reject a submitted timesheet for technician correction")
    public ResponseEntity<TimesheetResponse> reject(
            @PathVariable Long id,
            Authentication authentication
    ) {
        UserDetailsImpl user = principal(authentication);
        return ResponseEntity.ok(timesheetService.reject(id, user.getRole()));
    }

    @GetMapping("/{id}/export/csv")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    @Operation(summary = "Export a payroll-ready weekly timesheet as CSV")
    public ResponseEntity<byte[]> exportCsv(
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
