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
        event.setMessage(truncate(message, 500));
        event.setOldValue(truncate(oldValue, 255));
        event.setNewValue(truncate(newValue, 255));
        event.setActor(truncate(actor, 150));
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
    public void logCreated(WorkOrderEntity wo, String actor) {
        recordEvent(
                wo,
                WorkOrderEventType.CREATED,
                "Work order was created.",
                null,
                wo.getStatus() == null ? null : wo.getStatus().name(),
                actor
        );
    }

    @Transactional
    public void logStarted(WorkOrderEntity wo, WorkOrderStatus previousStatus) {
        String actor = getActor();

        recordEvent(
                wo,
                WorkOrderEventType.STARTED,
                "Technician started work.",
                null,
                WorkOrderStatus.IN_PROGRESS.name(),
                actor
        );

        recordEvent(
                wo,
                WorkOrderEventType.STATUS_CHANGED,
                "Status changed to IN_PROGRESS.",
                previousStatus == null ? null : previousStatus.name(),
                WorkOrderStatus.IN_PROGRESS.name(),
                actor
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

    @Transactional
    public void logReopened(WorkOrderEntity wo, String notes) {
        recordEvent(
                wo,
                WorkOrderEventType.REOPENED,
                notes == null || notes.isBlank()
                        ? "Work order was reopened by admin/dispatch."
                        : "Work order was reopened by admin/dispatch. Reason: " + notes,
                null,
                null,
                getActor()
        );
    }

    @Transactional
    public void logNoteAdded(WorkOrderEntity wo, String message, String actor) {
        recordEvent(
                wo,
                WorkOrderEventType.NOTE_ADDED,
                message,
                null,
                null,
                actor
        );
    }

    @Transactional
    public void logAttachmentAdded(WorkOrderEntity wo, String filename, String actor) {
        recordEvent(
                wo,
                WorkOrderEventType.ATTACHMENT_ADDED,
                filename == null || filename.isBlank()
                        ? "Attachment added to work order."
                        : "Attachment added: " + filename,
                null,
                filename,
                actor
        );
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
