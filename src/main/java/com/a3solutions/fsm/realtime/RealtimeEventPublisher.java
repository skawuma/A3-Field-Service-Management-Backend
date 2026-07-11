package com.a3solutions.fsm.realtime;

import com.a3solutions.fsm.auth.UserRepository;
import com.a3solutions.fsm.common.TextUtils;
import com.a3solutions.fsm.observability.FsmOperationalMetrics;
import com.a3solutions.fsm.technician.TechnicianEntity;
import com.a3solutions.fsm.technician.TechnicianRepository;
import com.a3solutions.fsm.workorder.WorkOrderEntity;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    public static final String USER_NOTIFICATIONS_QUEUE = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;
    private final TechnicianRepository technicianRepository;
    private final UserRepository userRepository;
    private final FsmOperationalMetrics metrics;
    private final Set<Long> publishedOverdueWorkOrderIds = ConcurrentHashMap.newKeySet();
    private final Set<Long> publishedNearBreachWorkOrderIds = ConcurrentHashMap.newKeySet();
    private final Set<Long> publishedExecutionBreachWorkOrderIds = ConcurrentHashMap.newKeySet();

    public RealtimeEventPublisher(
            SimpMessagingTemplate messagingTemplate,
            TechnicianRepository technicianRepository,
            UserRepository userRepository,
            FsmOperationalMetrics metrics
    ) {
        this.messagingTemplate = messagingTemplate;
        this.technicianRepository = technicianRepository;
        this.userRepository = userRepository;
        this.metrics = metrics;
    }

    public void publishDashboardEvent(RealtimeEventMessage message) {
        publishAfterCommit(DASHBOARD_TOPIC, message);
    }

    public void publishAlertEvent(RealtimeEventMessage message) {
        publishAfterCommit(ALERTS_TOPIC, message);
    }

    public void publishNewSlaBreaches(List<WorkOrderEntity> overdueWorkOrders, LocalDate currentDate) {
        publishNewSlaBreaches(overdueWorkOrders, currentDate, false);
    }

    public void publishNewSlaBreaches(
            List<WorkOrderEntity> overdueWorkOrders,
            LocalDate currentDate,
            boolean fullOverdueSnapshot
    ) {
        if (fullOverdueSnapshot) {
            Set<Long> currentOverdueIds = overdueWorkOrders.stream()
                    .map(WorkOrderEntity::getId)
                    .collect(java.util.stream.Collectors.toSet());

            publishedOverdueWorkOrderIds.retainAll(currentOverdueIds);
        }

        for (WorkOrderEntity workOrder : overdueWorkOrders) {
            if (workOrder.getId() == null || workOrder.getScheduledDate() == null) {
                continue;
            }

            if (!publishedOverdueWorkOrderIds.add(workOrder.getId())) {
                continue;
            }

            long overdueDays = ChronoUnit.DAYS.between(workOrder.getScheduledDate(), currentDate);
            metrics.recordSlaBreachPublished();
            publishSlaBreached(workOrder, overdueDays);
        }
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

        publishAssignedTechnicianNotification(workOrder, metadata);
    }

    public void publishWorkOrderCreated(WorkOrderEntity workOrder) {
        Map<String, Object> metadata = createWorkOrderMetadata(workOrder);
        metadata.put("eventKey", "created");

        String activityDescription = formatWorkOrderRef(workOrder.getId()) + " created for "
                + defaultText(workOrder.getClientName(), "customer");
        metadata.put("activityTitle", "Work order created");
        metadata.put("activityDescription", activityDescription);

        publishDashboardEvent(
                RealtimeEventMessage.of(
                        RealtimeEventType.WORK_ORDER_CREATED,
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
        metadata.put("slaBreached", Boolean.TRUE.equals(workOrder.getSlaBreached()));
        metadata.put("breachMinutes", workOrder.getBreachMinutes());
        metadata.put("actualCompletionMinutes", workOrder.getActualCompletionMinutes());

        String activityDescription = formatWorkOrderRef(workOrder.getId()) + " completed and signed";
        metadata.put("activityTitle", "Work order completed");
        metadata.put("activityDescription", activityDescription);

        RealtimeEventMessage completionMessage = RealtimeEventMessage.of(
                        RealtimeEventType.WORK_ORDER_COMPLETED,
                        activityDescription,
                        workOrder.getId(),
                        workOrder.getAssignedTechId(),
                        workOrder.getStatus() != null ? workOrder.getStatus().name() : null,
                        metadata
                );
        publishDashboardEvent(completionMessage);
        publishAlertEvent(completionMessage);
        publishedNearBreachWorkOrderIds.remove(workOrder.getId());
        publishedExecutionBreachWorkOrderIds.remove(workOrder.getId());
    }

    public void publishExecutionSlaState(WorkOrderEntity workOrder, Instant now) {
        if (workOrder.getId() == null || workOrder.getSlaDueAt() == null) {
            return;
        }
        Duration remaining = Duration.between(now, workOrder.getSlaDueAt());
        if (remaining.isNegative()) {
            if (publishedExecutionBreachWorkOrderIds.add(workOrder.getId())) {
                publishExecutionSlaAlert(workOrder, RealtimeEventType.SLA_BREACHED,
                        Math.max(0, -remaining.toMinutes()), "Execution SLA breached");
            }
            return;
        }
        if (remaining.compareTo(Duration.ofMinutes(30)) <= 0
                && publishedNearBreachWorkOrderIds.add(workOrder.getId())) {
            publishExecutionSlaAlert(workOrder, RealtimeEventType.SLA_NEAR_BREACH,
                    remaining.toMinutes(), "Execution SLA near breach");
        }
    }

    private void publishExecutionSlaAlert(WorkOrderEntity workOrder, RealtimeEventType type,
                                          long minutes, String title) {
        Map<String, Object> metadata = createWorkOrderMetadata(workOrder);
        metadata.put("eventKey", type == RealtimeEventType.SLA_BREACHED ? "sla_breach" : "sla_near_breach");
        metadata.put("activityTitle", title);
        metadata.put("slaDueAt", workOrder.getSlaDueAt().toString());
        metadata.put(type == RealtimeEventType.SLA_BREACHED ? "breachMinutes" : "minutesRemaining", minutes);
        String description = formatWorkOrderRef(workOrder.getId()) + (type == RealtimeEventType.SLA_BREACHED
                ? " breached execution SLA by " + minutes + " minutes"
                : " is due in " + minutes + " minutes");
        metadata.put("activityDescription", description);
        RealtimeEventMessage message = RealtimeEventMessage.of(type, description, workOrder.getId(),
                workOrder.getAssignedTechId(), workOrder.getStatus().name(), metadata);
        publishDashboardEvent(message);
        publishAlertEvent(message);
    }

    public void publishWorkOrderStarted(WorkOrderEntity workOrder, String previousStatus) {
        Map<String, Object> metadata = createWorkOrderMetadata(workOrder);
        String statusLabel = formatStatusLabel(workOrder.getStatus());
        String eventKey = switch (workOrder.getStatus()) {
            case EN_ROUTE -> "start_travel";
            case ARRIVED -> "arrive_onsite";
            case WORK_STARTED, IN_PROGRESS -> "start_work";
            default -> "status_change";
        };
        metadata.put("eventKey", eventKey);
        metadata.put("previousStatus", previousStatus);
        metadata.put("newStatus", workOrder.getStatus() != null ? workOrder.getStatus().name() : null);

        String activityDescription = formatWorkOrderRef(workOrder.getId()) + " marked " + statusLabel;
        metadata.put("activityTitle", statusLabel);
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
                    metrics.recordRealtimeEventPublished(destination);
                }
            });
            return;
        }

        messagingTemplate.convertAndSend(destination, message);
        metrics.recordRealtimeEventPublished(destination);
    }

    private void publishToUserAfterCommit(String username, String destination, RealtimeEventMessage message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messagingTemplate.convertAndSendToUser(username, destination, message);
                    metrics.recordRealtimeEventPublished(destination);
                }
            });
            return;
        }

        messagingTemplate.convertAndSendToUser(username, destination, message);
        metrics.recordRealtimeEventPublished(destination);
    }

    private void publishAssignedTechnicianNotification(
            WorkOrderEntity workOrder,
            Map<String, Object> workOrderMetadata
    ) {
        if (workOrder.getAssignedTechId() == null) {
            return;
        }

        technicianRepository.findById(workOrder.getAssignedTechId())
                .flatMap(technician -> technician.getUserId() != null
                        ? userRepository.findById(technician.getUserId())
                        : java.util.Optional.empty())
                .ifPresent(user -> {
                    Map<String, Object> metadata = new LinkedHashMap<>(workOrderMetadata);
                    metadata.put("eventKey", "technician_assignment_notification");
                    metadata.put("notificationTitle", "New Work Order Assigned");

                    String notificationMessage = "New Work Order Assigned: "
                            + formatWorkOrderRef(workOrder.getId()) + " "
                            + defaultText(workOrder.getClientName(), "customer");
                    metadata.put("notificationMessage", notificationMessage);

                    publishToUserAfterCommit(
                            user.getEmail(),
                            USER_NOTIFICATIONS_QUEUE,
                            RealtimeEventMessage.of(
                                    RealtimeEventType.WORK_ORDER_ASSIGNED,
                                    notificationMessage,
                                    workOrder.getId(),
                                    workOrder.getAssignedTechId(),
                                    workOrder.getStatus() != null ? workOrder.getStatus().name() : null,
                                    metadata
                            )
                    );
                });
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
        metadata.put("slaDueAt", workOrder.getSlaDueAt() == null ? null : workOrder.getSlaDueAt().toString());
        metadata.put("slaBreached", Boolean.TRUE.equals(workOrder.getSlaBreached()));
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
        return TextUtils.isBlank(trimmed) ? null : trimmed;
    }

    private String formatStatusLabel(WorkOrderStatus status) {
        if (status == null) {
            return "Unknown";
        }

        return switch (status) {
            case OPEN -> "Open";
            case ASSIGNED -> "Assigned";
            case EN_ROUTE -> "En Route";
            case ARRIVED -> "Arrived";
            case WORK_STARTED, IN_PROGRESS -> "Work Started";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
        };
    }
}
