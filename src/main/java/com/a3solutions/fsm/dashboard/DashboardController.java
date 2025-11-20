package com.a3solutions.fsm.dashboard;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.dashboard
 * @project A3 Field Service Management Backend
 * @date 11/19/25
 */
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> getSummary() {
        return ResponseEntity.ok(service.getSummary());
    }
}
