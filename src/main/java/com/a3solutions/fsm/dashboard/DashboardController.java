package com.a3solutions.fsm.dashboard;

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
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }


    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    public ResponseEntity<DashboardSummary> getSummary() {
        return ResponseEntity.ok(service.getSummary());
    }

    @GetMapping("/recent-activity")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    public ResponseEntity<List<DashboardRecentActivityItem>> getRecentActivity(
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(service.getRecentActivity(limit));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    public ResponseEntity<DashboardAnalytics> getAnalytics() {
        return ResponseEntity.ok(service.getAnalytics());
    }
    @GetMapping("/sla")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    public ResponseEntity<DashboardSlaSummary> getSlaSummary() {
        return ResponseEntity.ok(service.getSlaSummary());
    }
}
