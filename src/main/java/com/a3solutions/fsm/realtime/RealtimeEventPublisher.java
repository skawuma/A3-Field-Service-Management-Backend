package com.a3solutions.fsm.realtime;

import com.a3solutions.fsm.technician.TechnicianEntity;
import com.a3solutions.fsm.technician.TechnicianRepository;
import com.a3solutions.fsm.workorder.WorkOrderEntity;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.realtime
 * @project A3 Field Service Management Backend
 * @date 4/14/26
 */

/**
 * Publishes realtime events to WebSocket topics.
 *
 * Current-state design:
 * - Publishes directly to STOMP/WebSocket topics from the monolith.
 *
 * Future-state direction:
 * - This class can later be backed by Kafka/domain events without changing
 *   higher-level business service contracts.
 */
@Component
public class RealtimeEventPublisher {

    public static final String DASHBOARD_TOPIC = "/topic/dashboard";
    public static final String ALERTS_TOPIC = "/topic/alerts";

    private final SimpMessagingTemplate messagingTemplate;
    private final TechnicianRepository technicianRepository;

    public RealtimeEventPublisher(
            SimpMessagingTemplate messagingTemplate,
            TechnicianRepository technicianRepository
    ) {
        this.messagingTemplate = messagingTemplate;
        this.technicianRepository = technicianRepository;
    }

    public void publishDashboardEvent(RealtimeEventMessage message) {
        publishAfterCommit(DASHBOARD_TOPIC, message);
    }

    public void publishAlertEvent(RealtimeEventMessage message) {
        publishAfterCommit(ALERTS_TOPIC, message);
    }

    public void publishWorkOrderAssigned(
            WorkOrderEntity workOrder,
            Long previousTechnicianId,
            String previousStatus
    ) {
        Map<String, Object> metadata = createWorkOrderMetadata(workOrder);
        metadata.put("eventKey", "assignment");
        metadata.put("previousTechnicianId", previousTechnicianId);
        metadata.put("previousTechnicianName", resolveTechnicianName(previousTechnicianId));
        metadata.put("previousStatus", previousStatus);
        metadata.put("newStatus", workOrder.getStatus() != null ? workOrder.getStatus().name() : null);

        String activityDescription = formatWorkOrderRef(workOrder.getId()) + " assigned to "
                + defaultText(resolveTechnicianName(workOrder.getAssignedTechId()), "technician");
        metadata.put("activityTitle", "Technician assigned");
        metadata.put("activityDescription", activityDescription);

        publishDashboardEvent(
                RealtimeEventMessage.of(
                        RealtimeEventType.WORK_ORDER_ASSIGNED,
                        activityDescription,
                        workOrder.getId(),
                        workOrder.getAssignedTechId(),
                        workOrder.getStatus() != null ? workOrder.getStatus().name() : null,
                        metadata
                )
        );
    }

    public void publishWorkOrderCompleted(WorkOrderEntity workOrder) {
        Map<String, Object> metadata = createWorkOrderMetadata(workOrder);
        metadata.put("eventKey", "completion");
        metadata.put("completedAt", workOrder.getCompletedAt() != null ? workOrder.getCompletedAt().toString() : null);

        String activityDescription = formatWorkOrderRef(workOrder.getId()) + " completed and signed";
        metadata.put("activityTitle", "Work order completed");
        metadata.put("activityDescription", activityDescription);

        publishDashboardEvent(
                RealtimeEventMessage.of(
                        RealtimeEventType.WORK_ORDER_COMPLETED,
                        activityDescription,
                        workOrder.getId(),
                        workOrder.getAssignedTechId(),
                        workOrder.getStatus() != null ? workOrder.getStatus().name() : null,
                        metadata
                )
        );
    }

    public void publishWorkOrderStarted(WorkOrderEntity workOrder, String previousStatus) {
        Map<String, Object> metadata = createWorkOrderMetadata(workOrder);
        metadata.put("eventKey", "start");
        metadata.put("previousStatus", previousStatus);
        metadata.put("newStatus", workOrder.getStatus() != null ? workOrder.getStatus().name() : null);

        String activityDescription = formatWorkOrderRef(workOrder.getId()) + " marked In Progress";
        metadata.put("activityTitle", "Work order started");
        metadata.put("activityDescription", activityDescription);

        publishDashboardEvent(
                RealtimeEventMessage.of(
                        RealtimeEventType.WORK_ORDER_STATUS_CHANGED,
                        activityDescription,
                        workOrder.getId(),
                        workOrder.getAssignedTechId(),
                        workOrder.getStatus() != null ? workOrder.getStatus().name() : null,
                        metadata
                )
        );
    }

    public void publishWorkOrderReturnedToOpen(
            WorkOrderEntity workOrder,
            Long previousTechnicianId,
            String previousStatus,
            String reason
    ) {
        Map<String, Object> metadata = createWorkOrderMetadata(workOrder);
        metadata.put("eventKey", "returned_to_open");
        metadata.put("previousStatus", previousStatus);
        metadata.put("newStatus", workOrder.getStatus() != null ? workOrder.getStatus().name() : null);
        metadata.put("previousTechnicianId", previousTechnicianId);
        metadata.put("previousTechnicianName", resolveTechnicianName(previousTechnicianId));
        metadata.put("reason", normalizeOptionalText(reason));

        String activityDescription = formatWorkOrderRef(workOrder.getId()) + " returned to Open for reassignment";
        metadata.put("activityTitle", "Returned for reassignment");
        metadata.put("activityDescription", activityDescription);

        publishDashboardEvent(
                RealtimeEventMessage.of(
                        RealtimeEventType.WORK_ORDER_STATUS_CHANGED,
                        activityDescription,
                        workOrder.getId(),
                        null,
                        workOrder.getStatus() != null ? workOrder.getStatus().name() : null,
                        metadata
                )
        );
    }

    public void publishWorkOrderReopened(
            WorkOrderEntity workOrder,
            Long previousTechnicianId,
            String previousStatus,
            boolean hadSignature,
            boolean reasonProvided,
            String reason
    ) {
        Map<String, Object> metadata = createWorkOrderMetadata(workOrder);
        metadata.put("eventKey", "reopened");
        metadata.put("previousStatus", previousStatus);
        metadata.put("newStatus", workOrder.getStatus() != null ? workOrder.getStatus().name() : null);
        metadata.put("previousTechnicianId", previousTechnicianId);
        metadata.put("previousTechnicianName", resolveTechnicianName(previousTechnicianId));
        metadata.put("hadSignature", hadSignature);
        metadata.put("reasonProvided", reasonProvided);
        metadata.put("reason", normalizeOptionalText(reason));

        String activityDescription = formatWorkOrderRef(workOrder.getId()) + " reopened for dispatch";
        metadata.put("activityTitle", "Work order reopened");
        metadata.put("activityDescription", activityDescription);

        publishDashboardEvent(
                RealtimeEventMessage.of(
                        RealtimeEventType.WORK_ORDER_STATUS_CHANGED,
                        activityDescription,
                        workOrder.getId(),
                        null,
                        workOrder.getStatus() != null ? workOrder.getStatus().name() : null,
                        metadata
                )
        );
    }

    public void publishStructuredCompletionSubmitted(
            WorkOrderEntity workOrder,
            String faTag
    ) {
        Map<String, Object> metadata = createWorkOrderMetadata(workOrder);
        metadata.put("eventKey", "completion_report");
        metadata.put("faTag", faTag);

        String activityDescription = formatWorkOrderRef(workOrder.getId()) + " field report submitted";
        metadata.put("activityTitle", "Structured completion submitted");
        metadata.put("activityDescription", activityDescription);

        publishDashboardEvent(
                RealtimeEventMessage.of(
                        RealtimeEventType.WORK_ORDER_STATUS_CHANGED,
                        activityDescription,
                        workOrder.getId(),
                        workOrder.getAssignedTechId(),
                        workOrder.getStatus() != null ? workOrder.getStatus().name() : null,
                        metadata
                )
        );
    }

    public void publishSlaBreached(WorkOrderEntity workOrder, long overdueDays) {
        Map<String, Object> metadata = createWorkOrderMetadata(workOrder);
        metadata.put("eventKey", "sla_breach");
        metadata.put("overdueDays", overdueDays);
        metadata.put("activityTitle", "SLA breached");

        String activityDescription = formatWorkOrderRef(workOrder.getId()) + " is "
                + overdueDays + " day" + (overdueDays == 1 ? "" : "s") + " overdue";
        metadata.put("activityDescription", activityDescription);

        RealtimeEventMessage message = RealtimeEventMessage.of(
                RealtimeEventType.SLA_BREACHED,
                activityDescription,
                workOrder.getId(),
                workOrder.getAssignedTechId(),
                workOrder.getStatus() != null ? workOrder.getStatus().name() : null,
                metadata
        );

        publishDashboardEvent(message);
        publishAlertEvent(message);
    }

    private void publishAfterCommit(String destination, RealtimeEventMessage message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messagingTemplate.convertAndSend(destination, message);
                }
            });
            return;
        }

        messagingTemplate.convertAndSend(destination, message);
    }

    private Map<String, Object> createWorkOrderMetadata(WorkOrderEntity workOrder) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        String clientName = defaultText(workOrder.getClientName(), "Unknown customer");

        metadata.put("workOrderRef", formatWorkOrderRef(workOrder.getId()));
        metadata.put("clientName", clientName);
        metadata.put("customerName", clientName);
        metadata.put("title", abbreviate(workOrder.getDescription(), 60));
        metadata.put("description", defaultText(workOrder.getDescription(), ""));
        metadata.put("scheduledDate", formatDate(workOrder.getScheduledDate()));
        metadata.put("priority", defaultText(workOrder.getPriority(), "UNSPECIFIED"));
        metadata.put("assignedTechId", workOrder.getAssignedTechId());
        metadata.put("assignedTechName", resolveTechnicianName(workOrder.getAssignedTechId()));
        metadata.put("statusLabel", formatStatusLabel(workOrder.getStatus()));
        return metadata;
    }

    private String resolveTechnicianName(Long technicianId) {
        if (technicianId == null) {
            return null;
        }

        return technicianRepository.findById(technicianId)
                .map(this::formatTechnicianName)
                .orElse("Technician #" + technicianId);
    }

    private String formatTechnicianName(TechnicianEntity technician) {
        if (technician == null) {
            return null;
        }

        String fullName = normalizeOptionalText(technician.getFullName());
        if (fullName != null) {
            return fullName;
        }

        String email = normalizeOptionalText(technician.getEmail());
        if (email != null) {
            return email;
        }

        return "Technician #" + technician.getId();
    }

    private String formatWorkOrderRef(Long workOrderId) {
        return workOrderId != null ? "WO-" + workOrderId : "Work order";
    }

    private String abbreviate(String value, int maxLength) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            return "No description";
        }

        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength) + "...";
    }

    private String formatDate(LocalDate value) {
        return value != null ? value.toString() : null;
    }

    private String defaultText(String value, String fallback) {
        String normalized = normalizeOptionalText(value);
        return normalized != null ? normalized : fallback;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String formatStatusLabel(WorkOrderStatus status) {
        if (status == null) {
            return "Unknown";
        }

        return switch (status) {
            case OPEN -> "Open";
            case ASSIGNED -> "Assigned";
            case IN_PROGRESS -> "In Progress";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
        };
    }
}
