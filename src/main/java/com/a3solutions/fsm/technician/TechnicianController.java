package com.a3solutions.fsm.technician;

import com.a3solutions.fsm.common.PageResponse;
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
    public ResponseEntity<PageResponse<TechnicianDto>> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,

            // NEW — unified sorting format
            @RequestParam(defaultValue = "lastName,asc") String sort

    ) {
        return ResponseEntity.ok(
                technicianService.getPage(page, size, sort)
        );
    }


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TechnicianDto> create(@RequestBody TechnicianCreateRequest request) {
        return ResponseEntity.ok(technicianService.create(request));
    }
    // 🔥 NEW: UPDATE TECHNICIAN (ADMIN ONLY)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TechnicianDto> update(
            @PathVariable Long id,
            @RequestBody TechnicianCreateRequest request
    ) {
        return ResponseEntity.ok(technicianService.update(id, request));
    }

    // 🔥 NEW: DELETE TECHNICIAN (ADMIN ONLY)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        technicianService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
