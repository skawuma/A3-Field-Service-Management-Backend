package com.a3solutions.fsm.workorder;

import com.a3solutions.fsm.auth.UserDetailsImpl;
import com.a3solutions.fsm.common.PageResponse;
import com.a3solutions.fsm.security.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */

@RestController
@RequestMapping("/api/workorders")
public class WorkOrderController {

    private final WorkOrderService service;

    public WorkOrderController(WorkOrderService service) {
        this.service = service;
    }

    // =====================================================================
    // GET PAGE
    // =====================================================================
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<PageResponse<WorkOrderDto>> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "id,desc") String sort,
            @RequestParam(required = false) Long technicianId,
            Authentication auth
    ) {

        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        Role role = user.getRole();

        if (role == Role.TECH) {
            technicianId = user.getId();
        }

        return ResponseEntity.ok(
                service.getPage(page, size, search, priority, status, sort, technicianId)
        );
    }

    // =====================================================================
    // GET ONE — TECH ONLY IF ASSIGNED
    // =====================================================================
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<?> getOne(
            @PathVariable Long id,
            Authentication auth
    ) {
        var dto = service.getById(id);

        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        Role role = user.getRole();

        if (role == Role.TECH) {
            if (dto.assignedTechId() == null || !dto.assignedTechId().equals(user.getId())) {
                return ResponseEntity.status(403).body("Not authorized to view this work order.");
            }
        }

        return ResponseEntity.ok(dto);
    }

    // =====================================================================
    // CREATE
    // =====================================================================
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    public ResponseEntity<WorkOrderDto> create(@RequestBody WorkOrderCreateRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    // =====================================================================
    // ASSIGN TECHNICIAN
    // =====================================================================
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    public ResponseEntity<WorkOrderDto> assignTechnician(
            @PathVariable Long id,
            @RequestBody AssignTechnicianRequest request
    ) {
        return ResponseEntity.ok(service.assignTechnician(id, request));
    }

    // =====================================================================
    // UPDATE WORK ORDER — TECH LIMITED
    // =====================================================================
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody WorkOrderCreateRequest req,
            Authentication auth
    ) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        Role role = user.getRole();

        if (role == Role.TECH) {

            if (!service.isAssignedToTech(id, user.getId())) {
                return ResponseEntity.status(403).body("TECH can only update assigned work orders.");
            }

            return ResponseEntity.ok(service.updateTech(id, req, user.getId()));
        }

        return ResponseEntity.ok(service.updateAdmin(id, req));
    }
}