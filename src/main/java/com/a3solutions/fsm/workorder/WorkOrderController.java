package com.a3solutions.fsm.workorder;

import com.a3solutions.fsm.auth.UserDetailsImpl;
import com.a3solutions.fsm.common.PageResponse;
import com.a3solutions.fsm.security.JwtService;
import com.a3solutions.fsm.security.Role;
import com.a3solutions.fsm.workordercompletion.WorkOrderCompletionRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
   private final JwtService jwtService;
    public WorkOrderController(WorkOrderService service, JwtService jwtService) {
        this.service = service;
        this.jwtService = jwtService;
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

        //🔥 TECH: map userId -> technicianId
        if (role == Role.TECH) {
            technicianId = service.findTechnicianIdForUser(user.getId());
        }

        return ResponseEntity.ok(
                service.getPage(page, size, search, priority, status, sort, technicianId)
        );
    }

    // =====================================================================
    // GET ONE — TECH ONLY IF ASSIGNED
    // =====================================================================
//    @GetMapping("/{id}")
//    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
//    public ResponseEntity<?> getOne(
//            @PathVariable Long id,
//            Authentication auth
//    ) {
//        var dto = service.getById(id);
//
//        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
//        Role role = user.getRole();
//
//        if (role == Role.TECH) {
//            if (dto.assignedTechId() == null || !dto.assignedTechId().equals(user.getId())) {
//                return ResponseEntity.status(403).body("Not authorized to view this work order.");
//            }
//        }
//
//        return ResponseEntity.ok(dto);
//    }


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<?> getOne(
            @PathVariable Long id,
            Authentication auth
    ) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        Role role = user.getRole();

        if (role == Role.TECH) {
            boolean allowed = service.canTechAccessWorkOrder(id, user.getId());
            if (!allowed) {
                return ResponseEntity.status(403)
                        .body("Not authorized to view this work order.");
            }
        }

        var dto = service.getById(id);
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
            @RequestBody AssignTechnicianRequest request,
            HttpServletRequest httpReq
    ) {
        // Extract username/email from JWT
        String authHeader = httpReq.getHeader("Authorization");
        String token = authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : null;

//        String actor = jwtService.extractUsername(token);

        // User's email from Security Context
        String actor = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return ResponseEntity.ok(service.assignTechnician(id, request, actor));
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

            if (!service.canTechAccessWorkOrder(id, user.getId())) {
                return ResponseEntity.status(403).body("TECH can only update assigned work orders.");
            }

            return ResponseEntity.ok(service.updateTechByUser(id, req, user.getId()));
        }

        return ResponseEntity.ok(service.updateAdmin(id, req));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('TECH')")
    public ResponseEntity<?> startWorkOrder(
            @PathVariable Long id,
            Authentication auth
    ) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();

        if (!service.canTechAccessWorkOrder(id, user.getId())) {
            return ResponseEntity.status(403)
                    .body("TECH can only start assigned work orders.");
        }

        return ResponseEntity.ok(service.startWorkOrder(id, user.getId()));
    }

    @PostMapping("/{id}/return-to-open")
    @PreAuthorize("hasRole('TECH')")
    public ResponseEntity<?> returnWorkOrderToOpen(
            @PathVariable Long id,
            @RequestBody(required = false) ReturnToOpenRequest request,
            Authentication auth
    ) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();

        if (!service.canTechAccessWorkOrder(id, user.getId())) {
            return ResponseEntity.status(403)
                    .body("TECH can only release assigned work orders.");
        }

        String reason = request != null ? request.reason() : null;
        return ResponseEntity.ok(service.returnWorkOrderToOpen(id, user.getId(), reason));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('TECH')")
    public ResponseEntity<?> completeWorkOrder(
            @PathVariable Long id,
            @RequestBody CompleteWorkOrderRequest req,
            Authentication auth
    ) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();

        if (!service.canTechAccessWorkOrder(id, user.getId())) {
            return ResponseEntity.status(403)
                    .body("TECH can only complete assigned work orders.");
        }

        return ResponseEntity.ok(
                service.completeWorkOrder(id, req, user.getId())
        );
    }

    @PostMapping("/{id}/completion-report")
    @PreAuthorize("hasRole('TECH')")
    public ResponseEntity<?> submitCompletionReport(
            @PathVariable Long id,
            @RequestBody WorkOrderCompletionRequest request,
            Authentication auth
    ) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();

        if (!service.canTechAccessWorkOrder(id, user.getId())) {
            return ResponseEntity.status(403)
                    .body("TECH can only submit reports for assigned work orders.");
        }

        return ResponseEntity.ok(
                service.submitStructuredCompletionReport(id, request, user.getId())
        );
    }

    @GetMapping("/{id}/completion-report")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<?> getCompletionReport(
            @PathVariable Long id,
            Authentication auth
    ) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        Role role = user.getRole();

        if (role == Role.TECH && !service.canTechAccessWorkOrder(id, user.getId())) {
            return ResponseEntity.status(403)
                    .body("TECH can only view reports for assigned work orders.");
        }

        return ResponseEntity.ok(
                service.getCompletionByWorkOrderId(id)
        );
    }

    @GetMapping("/{id}/signature")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<?> getSignature(
            @PathVariable Long id,
            Authentication auth
    ) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        Role role = user.getRole();

        if (role == Role.TECH && !service.canTechAccessWorkOrder(id, user.getId())) {
            return ResponseEntity.status(403).body("TECH can only view signature for assigned work orders!!");
        }

        return service.getSignature(id);
    }
    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    public ResponseEntity<WorkOrderDto> reopenWorkOrder(
            @PathVariable Long id,
            @RequestBody(required = false) ReopenWorkOrderRequest request
    ) {
        String reason = request != null ? request.reason() : null;
        return ResponseEntity.ok(service.reopenWorkOrder(id, reason));
    }

}
