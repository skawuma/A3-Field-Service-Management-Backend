package com.a3solutions.fsm.technician;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.technician
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
@RestController
@RequestMapping("/api/technicians")
public class TechnicianController {
    private final TechnicianService technicianService;

    public TechnicianController(TechnicianService technicianService) {
        this.technicianService = technicianService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    public ResponseEntity<List<TechnicianDto>> getAll() {
        return ResponseEntity.ok(technicianService.getAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TechnicianDto> create(@RequestBody TechnicianCreateRequest request) {
        return ResponseEntity.ok(technicianService.create(request));
    }

    // GET by id, PUT, DELETE, etc...
}
