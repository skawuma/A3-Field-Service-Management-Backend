package com.a3solutions.fsm.workorder;


import com.a3solutions.fsm.auth.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3_SOLUTIONS_PROJECT
 * @date 11/26/25
 */

@Service
public class WorkOrderEventService {

    private final WorkOrderEventRepository repository;

    public WorkOrderEventService(WorkOrderEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public WorkOrderEventEntity recordEvent(
            WorkOrderEntity workOrder,
            WorkOrderEventType type,
            String message,
            String oldValue,
            String newValue,
            String actor
    ) {
        WorkOrderEventEntity event = new WorkOrderEventEntity();
        event.setWorkOrder(workOrder);
        event.setEventType(type);
        event.setMessage(message);
        event.setOldValue(oldValue);
        event.setNewValue(newValue);
        event.setActor(actor);
        return repository.save(event);
    }

    @Transactional(readOnly = true)
    public List<WorkOrderEventEntity> getTimeline(Long workOrderId) {
        return repository.findByWorkOrderIdOrderByCreatedAtAsc(workOrderId);
    }

    // Simple mapper to DTO
    public WorkOrderEventDto toDto(WorkOrderEventEntity e) {
        WorkOrderEventDto dto = new WorkOrderEventDto();
        dto.setId(e.getId());
        dto.setEventType(e.getEventType());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setMessage(e.getMessage());
        dto.setOldValue(e.getOldValue());
        dto.setNewValue(e.getNewValue());
        dto.setActor(e.getActor());
        return dto;
    }

    @Transactional
    public void logCompleted(
            WorkOrderEntity wo,
            String notes
    ) {
        recordEvent(
                wo,
                WorkOrderEventType.COMPLETED,
                notes == null || notes.isBlank()
                        ? "Work order completed and signed off."
                        : "Work order completed and signed off. Notes: " + notes,
                null,
                null,
                getActor()
        );
    }

    @Transactional
    public void logStarted(WorkOrderEntity wo, WorkOrderStatus previousStatus) {
        recordEvent(
                wo,
                WorkOrderEventType.STATUS_CHANGED,
                "Technician started work. Status changed to IN_PROGRESS.",
                previousStatus == null ? null : previousStatus.name(),
                WorkOrderStatus.IN_PROGRESS.name(),
                getActor()
        );
    }

    /**
     * Implemented audit logging using Spring Security context to dynamically resolve
     * the authenticated user at the service layer,
     * ensuring decoupling from controllers and future compatibility with microservices.
     * @return
     */
    private String getActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return "SYSTEM";
        }

        Object principal = auth.getPrincipal();

        // If you're using your custom UserDetails
        if (principal instanceof UserDetailsImpl user) {
            return user.getUsername(); // or user.getEmail()
        }

        return principal.toString();
    }
}
