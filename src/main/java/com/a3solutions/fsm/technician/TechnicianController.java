package com.a3solutions.fsm.technician;

import com.a3solutions.fsm.common.PageResponse;
import com.a3solutions.fsm.exceptions.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.technician
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
@RestController
@RequestMapping("/api/technicians")
@Tag(
        name = "Technicians",
        description = "Technician directory and technician profile management."
)
@SecurityRequirement(name = "bearerAuth")
public class TechnicianController {

    private final TechnicianService technicianService;

    public TechnicianController(TechnicianService technicianService) {
        this.technicianService = technicianService;
    }

    @Operation(
            summary = "List technicians",
            description = "Returns a paginated list of technicians for ADMIN and DISPATCH users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Technicians returned", content = @Content(schema = @Schema(implementation = TechnicianPageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not allowed to list technicians", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    public ResponseEntity<PageResponse<TechnicianDto>> getPage(
            @Parameter(description = "Zero-based page index.", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size.", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction.", example = "lastName,asc") @RequestParam(defaultValue = "lastName,asc") String sort
    ) {
        return ResponseEntity.ok(
                technicianService.getPage(page, size, sort)
        );
    }

    @Operation(
            summary = "Get a technician profile",
            description = "Returns a single technician record by id."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Technician returned", content = @Content(schema = @Schema(implementation = TechnicianDto.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Technician not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<TechnicianDto> getOne(
            @Parameter(description = "Technician identifier.", example = "3") @PathVariable Long id
    ) {
        var tech = technicianService.getById(id);
        return ResponseEntity.ok(tech);
    }


    @Operation(
            summary = "Create a technician",
            description = "Creates a technician profile. Restricted to ADMIN users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Technician created", content = @Content(schema = @Schema(implementation = TechnicianDto.class))),
            @ApiResponse(responseCode = "400", description = "Technician request is invalid", content = @Content(schema = @Schema(implementation = com.a3solutions.fsm.exceptions.ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not allowed to create technicians", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TechnicianDto> create(@Valid @RequestBody TechnicianCreateRequest request) {
        return ResponseEntity.ok(technicianService.create(request));
    }

    @Operation(
            summary = "Update a technician",
            description = "Updates an existing technician profile. Restricted to ADMIN users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Technician updated", content = @Content(schema = @Schema(implementation = TechnicianDto.class))),
            @ApiResponse(responseCode = "400", description = "Technician request is invalid", content = @Content(schema = @Schema(implementation = com.a3solutions.fsm.exceptions.ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not allowed to update technicians", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Technician not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TechnicianDto> update(
            @Parameter(description = "Technician identifier.", example = "3") @PathVariable Long id,
            @Valid @RequestBody TechnicianCreateRequest request
    ) {
        return ResponseEntity.ok(technicianService.update(id, request));
    }

    @Operation(
            summary = "Delete a technician",
            description = "Deletes a technician profile. Restricted to ADMIN users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Technician deleted"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not allowed to delete technicians", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Technician not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Technician identifier.", example = "3") @PathVariable Long id
    ) {
        technicianService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
