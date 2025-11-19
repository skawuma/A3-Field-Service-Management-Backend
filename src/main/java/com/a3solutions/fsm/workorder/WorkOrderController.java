package com.a3solutions.fsm.workorder;

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

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<List<WorkOrderDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    public ResponseEntity<WorkOrderDto> create(@RequestBody WorkOrderCreateRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    // GET/{id}, PUT/{id}, DELETE/{id} same pattern
}
