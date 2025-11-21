package com.a3solutions.fsm.workorder;

import com.a3solutions.fsm.common.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * MAIN PAGINATION, FILTERING & SORTING ENDPOINT
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<PageResponse<WorkOrderDto>> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,

            // NEW — filters
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String status,

            // NEW — sorting
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        return ResponseEntity.ok(
                service.getPage(page, size, search, priority, status, sort)
        );
    }


    /**
     * CREATE WORK ORDER
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    public ResponseEntity<WorkOrderDto> create(@RequestBody WorkOrderCreateRequest req) {
        return ResponseEntity.ok(service.create(req));
    }


    /**
     * ASSIGN TECHNICIAN
     */
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    public ResponseEntity<WorkOrderDto> assignTechnician(
            @PathVariable Long id,
            @RequestBody AssignTechnicianRequest request
    ) {
        return ResponseEntity.ok(service.assignTechnician(id, request));
    }

    // (Optional) FUTURE: GET/{id}, UPDATE/{id}, DELETE/{id}



}

