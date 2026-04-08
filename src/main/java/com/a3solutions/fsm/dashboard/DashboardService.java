package com.a3solutions.fsm.dashboard;

import com.a3solutions.fsm.auth.UserRepository;
import com.a3solutions.fsm.technician.TechnicianRepository;
import com.a3solutions.fsm.workorder.WorkOrderEntity;
import com.a3solutions.fsm.workorder.WorkOrderEventEntity;
import com.a3solutions.fsm.workorder.WorkOrderEventRepository;
import com.a3solutions.fsm.workorder.WorkOrderEventType;
import com.a3solutions.fsm.workorder.WorkOrderRepository;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.dashboard
 * @project A3 Field Service Management Backend
 * @date 11/19/25
 */
@Service
public class DashboardService {


    private static final List<WorkOrderStatus> TERMINAL_STATUSES = List.of(
            WorkOrderStatus.COMPLETED,
            WorkOrderStatus.CANCELLED
    );
    private static final List<String> STATUS_BUCKET_ORDER = List.of(
            "OPEN",
            "ASSIGNED",
            "IN_PROGRESS",
            "COMPLETED",
            "CANCELLED"
    );
    private static final List<String> PRIORITY_BUCKET_ORDER = List.of("LOW", "MEDIUM", "HIGH", "CRITICAL", "UNSPECIFIED");
    private static final int COMPLETION_TREND_DAYS = 7;
    private static final DateTimeFormatter TREND_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM d");

    private final TechnicianRepository techRepo;
    private final WorkOrderRepository woRepo;
    private final WorkOrderEventRepository workOrderEventRepository;
    private final UserRepository userRepo;
    private static final int SLA_LIST_LIMIT = 5;
    private static final List<WorkOrderStatus> ACTIVE_SLA_STATUSES = List.of(
            WorkOrderStatus.OPEN,
            WorkOrderStatus.ASSIGNED,
            WorkOrderStatus.IN_PROGRESS
    );

    public DashboardService(
            TechnicianRepository techRepo,
            WorkOrderRepository woRepo,
            WorkOrderEventRepository workOrderEventRepository,
            UserRepository userRepo
    ) {
        this.techRepo = techRepo;
        this.woRepo = woRepo;
        this.workOrderEventRepository = workOrderEventRepository;
        this.userRepo = userRepo;
    }

    public DashboardSummary getSummary() {
        long totalTechs = techRepo.count();
        long totalWO = woRepo.count();
        long open = woRepo.countByStatus(WorkOrderStatus.OPEN);
        long inProgress = woRepo.countByStatus(WorkOrderStatus.IN_PROGRESS);
        long unassigned = woRepo.countByAssignedTechIdIsNull();

        ZoneId zone = ZoneId.systemDefault();
        LocalDate currentDate = LocalDate.now(zone);
        long scheduledToday = woRepo.countByScheduledDate(currentDate);
        long dueToday = woRepo.countByScheduledDateAndStatusNotIn(currentDate, TERMINAL_STATUSES);
        long overdue = woRepo.countByScheduledDateBeforeAndStatusNotIn(currentDate, TERMINAL_STATUSES);
        Instant startOfDay = currentDate.atStartOfDay(zone).toInstant();
        Instant startOfTomorrow = currentDate.plusDays(1).atStartOfDay(zone).toInstant();

        long completedToday = woRepo.countByStatusAndCompletedAtBetween(
                WorkOrderStatus.COMPLETED,
                startOfDay,
                startOfTomorrow
        );

        long highPriorityOpen = woRepo.countByStatusAndPriorityIn(
                WorkOrderStatus.OPEN,
                List.of("HIGH", "CRITICAL")
        );

        return new DashboardSummary(
                totalTechs,
                totalWO,
                open,
                inProgress,
                unassigned,
                scheduledToday,
                dueToday,
                overdue,
                completedToday,
                highPriorityOpen
        );
    }

    public DashboardAnalytics getAnalytics() {
        Map<String, Long> rawStatusCounts = woRepo.countWorkOrdersByStatus()
                .stream()
                .collect(Collectors.toMap(
                        DashboardBucketProjection::getBucketKey,
                        DashboardBucketProjection::getTotal
                ));

        Map<String, Long> rawPriorityCounts = woRepo.countWorkOrdersByPriority()
                .stream()
                .collect(Collectors.toMap(
                        DashboardBucketProjection::getBucketKey,
                        DashboardBucketProjection::getTotal
                ));

        Map<String, Long> statusCounts = normalizeStatusBuckets(rawStatusCounts);
        Map<String, Long> priorityCounts = normalizePriorityBuckets(rawPriorityCounts);

        ZoneId zone = ZoneId.systemDefault();
        LocalDate currentDate = LocalDate.now(zone);
        LocalDate startDate = currentDate.minusDays(COMPLETION_TREND_DAYS - 1L);
        Instant trendStart = startDate.atStartOfDay(zone).toInstant();
        Instant trendEnd = currentDate.plusDays(1L).atStartOfDay(zone).toInstant();

        Map<LocalDate, Long> completionCounts = woRepo.countCompletionsByCompletedDate(trendStart, trendEnd)
                .stream()
                .collect(Collectors.toMap(
                        DashboardDateBucketProjection::getBucketDate,
                        DashboardDateBucketProjection::getTotal
                ));

        List<DashboardChartDatum> workOrdersByStatus = statusCounts.entrySet()
                .stream()
                .map(entry -> new DashboardChartDatum(
                        entry.getKey(),
                        formatStatusLabel(WorkOrderStatus.valueOf(entry.getKey())),
                        entry.getValue()
                ))
                .toList();

        List<DashboardChartDatum> workOrdersByPriority = priorityCounts.entrySet()
                .stream()
                .map(entry -> new DashboardChartDatum(
                        entry.getKey(),
                        formatPriorityLabel(entry.getKey()),
                        entry.getValue()
                ))
                .toList();

        List<DashboardTrendPoint> completionTrend = startDate.datesUntil(currentDate.plusDays(1L))
                .map(day -> new DashboardTrendPoint(
                        day,
                        day.format(TREND_LABEL_FORMATTER),
                        completionCounts.getOrDefault(day, 0L)
                ))
                .toList();

        return new DashboardAnalytics(workOrdersByStatus, workOrdersByPriority, completionTrend);
    }

    public List<DashboardRecentActivityItem> getRecentActivity(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        int fetchSize = Math.max(safeLimit * 3, 20);

        return workOrderEventRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, fetchSize))
                .stream()
                .filter(event -> shouldDisplayOnDashboard(event.getEventType()))
                .limit(safeLimit)
                .map(this::toRecentActivityItem)
                .toList();
    }

    private boolean shouldDisplayOnDashboard(WorkOrderEventType eventType) {
        return eventType == WorkOrderEventType.CREATED
                || eventType == WorkOrderEventType.ASSIGNED_TECHNICIAN
                || eventType == WorkOrderEventType.UNASSIGNED_TECHNICIAN
                || eventType == WorkOrderEventType.STARTED
                || eventType == WorkOrderEventType.COMPLETED
                || eventType == WorkOrderEventType.REOPENED;
    }

    private DashboardRecentActivityItem toRecentActivityItem(WorkOrderEventEntity event) {
        WorkOrderEntity workOrder = event.getWorkOrder();
        Long workOrderId = workOrder != null ? workOrder.getId() : null;
        String workOrderRef = workOrderId != null ? "WO-" + workOrderId : "Work order";

        String title = buildTitle(event.getEventType());
        String description = buildDescription(workOrderRef, event);

        return new DashboardRecentActivityItem(
                workOrderId,
                event.getEventType().name(),
                title,
                description,
                event.getActor(),
                event.getCreatedAt()
        );
    }

    private String buildTitle(WorkOrderEventType eventType) {
        return switch (eventType) {
            case CREATED -> "Work order created";
            case ASSIGNED_TECHNICIAN -> "Technician assigned";
            case UNASSIGNED_TECHNICIAN -> "Returned for reassignment";
            case STARTED -> "Work order started";
            case COMPLETED -> "Work order completed";
            case REOPENED -> "Work order reopened";
            default -> "Work order activity";
        };
    }

    private String buildDescription(String workOrderRef, WorkOrderEventEntity event) {
        return switch (event.getEventType()) {
            case CREATED -> workOrderRef + " created";
            case ASSIGNED_TECHNICIAN -> workOrderRef + " assigned to " + extractTechnicianName(event.getMessage());
            case UNASSIGNED_TECHNICIAN -> workOrderRef + " returned to Open for reassignment";
            case STARTED -> workOrderRef + " marked In Progress";
            case COMPLETED -> workOrderRef + " completed and signed";
            case REOPENED -> workOrderRef + " reopened for dispatch";
            default -> event.getMessage() != null ? event.getMessage() : workOrderRef + " activity recorded";
        };
    }

    private String extractTechnicianName(String message) {
        if (message == null || message.isBlank()) {
            return "technician";
        }

        String prefix = "Technician ";
        String suffix = " was assigned to this work order.";

        if (message.startsWith(prefix) && message.endsWith(suffix)) {
            return message.substring(prefix.length(), message.length() - suffix.length());
        }

        return "technician";
    }

    private String formatStatusLabel(WorkOrderStatus status) {
        return switch (status) {
            case OPEN -> "Open";
            case ASSIGNED -> "Assigned";
            case IN_PROGRESS -> "In Progress";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
        };
    }

    private String formatPriorityLabel(String priority) {
        return switch (priority) {
            case "LOW" -> "Low";
            case "MEDIUM" -> "Medium";
            case "HIGH" -> "High";
            case "CRITICAL" -> "Critical";
            default -> "Unspecified";
        };
    }

    private Map<String, Long> normalizeStatusBuckets(Map<String, Long> raw) {
        return normalizeBuckets(raw, STATUS_BUCKET_ORDER);
    }

    private Map<String, Long> normalizePriorityBuckets(Map<String, Long> raw) {
        return normalizeBuckets(raw, PRIORITY_BUCKET_ORDER);
    }

    private Map<String, Long> normalizeBuckets(Map<String, Long> raw, List<String> orderedKeys) {
        LinkedHashMap<String, Long> normalized = new LinkedHashMap<>();

        for (String key : orderedKeys) {
            normalized.put(key, raw.getOrDefault(key, 0L));
        }

        return normalized;

    }

    public DashboardSlaSummary getSlaSummary() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate currentDate = LocalDate.now(zone);

        if (hasRole("TECH")) {
            Long technicianId = getCurrentTechnicianId();
            return getTechnicianSlaSummary(technicianId, currentDate);
        }

        return getGlobalSlaSummary(currentDate);
    }

    public List<DashboardTechnicianWorkloadItem> getTechnicianWorkload() {
        LocalDate currentDate = LocalDate.now(ZoneId.systemDefault());
        List<String> terminalStatusNames = TERMINAL_STATUSES.stream()
                .map(Enum::name)
                .toList();

        return woRepo.findTechnicianWorkloads(currentDate, terminalStatusNames)
                .stream()
                .map(this::toTechnicianWorkloadItem)
                .toList();
    }



    private DashboardSlaSummary getGlobalSlaSummary(LocalDate currentDate) {
        long dueToday = woRepo.countByScheduledDateAndStatusNotIn(currentDate, TERMINAL_STATUSES);
        long overdue = woRepo.countByScheduledDateBeforeAndStatusNotIn(currentDate, TERMINAL_STATUSES);

        List<WorkOrderEntity> overdueWorkOrders = woRepo
                .findByScheduledDateBeforeAndStatusInOrderByScheduledDateAscIdAsc(
                        currentDate,
                        ACTIVE_SLA_STATUSES,
                        PageRequest.of(0, SLA_LIST_LIMIT)
                )
                .stream()
                .toList();

        List<WorkOrderEntity> dueTodayWorkOrders = woRepo
                .findByScheduledDateAndStatusInOrderByIdDesc(
                        currentDate,
                        ACTIVE_SLA_STATUSES,
                        PageRequest.of(0, SLA_LIST_LIMIT)
                )
                .stream()
                .toList();

        List<DashboardSlaWorkOrderItem> overdueItems = toSlaItems(overdueWorkOrders, currentDate);
        List<DashboardSlaWorkOrderItem> dueTodayItems = toSlaItems(dueTodayWorkOrders, currentDate);

        return new DashboardSlaSummary(
                overdue,
                dueToday,
                overdueItems,
                dueTodayItems
        );
    }

    private DashboardSlaSummary getTechnicianSlaSummary(Long technicianId, LocalDate currentDate) {
        long dueToday = woRepo.countByAssignedTechIdAndScheduledDateAndStatusNotIn(
                technicianId,
                currentDate,
                TERMINAL_STATUSES
        );

        long overdue = woRepo.countByAssignedTechIdAndScheduledDateBeforeAndStatusNotIn(
                technicianId,
                currentDate,
                TERMINAL_STATUSES
        );

        List<WorkOrderEntity> overdueWorkOrders = woRepo
                .findByAssignedTechIdAndScheduledDateBeforeAndStatusInOrderByScheduledDateAscIdAsc(
                        technicianId,
                        currentDate,
                        ACTIVE_SLA_STATUSES,
                        PageRequest.of(0, SLA_LIST_LIMIT)
                )
                .stream()
                .toList();

        List<WorkOrderEntity> dueTodayWorkOrders = woRepo
                .findByAssignedTechIdAndScheduledDateAndStatusInOrderByIdDesc(
                        technicianId,
                        currentDate,
                        ACTIVE_SLA_STATUSES,
                        PageRequest.of(0, SLA_LIST_LIMIT)
                )
                .stream()
                .toList();

        List<DashboardSlaWorkOrderItem> overdueItems = toSlaItems(overdueWorkOrders, currentDate);
        List<DashboardSlaWorkOrderItem> dueTodayItems = toSlaItems(dueTodayWorkOrders, currentDate);

        return new DashboardSlaSummary(
                overdue,
                dueToday,
                overdueItems,
                dueTodayItems
        );
    }

    private DashboardTechnicianWorkloadItem toTechnicianWorkloadItem(
            DashboardTechnicianWorkloadProjection projection
    ) {
        Long technicianId = projection.getTechnicianId();
        String technicianName = projection.getTechnicianName();

        if (technicianName == null || technicianName.isBlank()) {
            technicianName = technicianId != null ? "Technician #" + technicianId : "Technician";
        }

        return new DashboardTechnicianWorkloadItem(
                technicianId,
                technicianName,
                projection.getTotalAssignedWorkOrders(),
                projection.getOpenAssignedWorkOrders(),
                projection.getInProgressAssignedWorkOrders(),
                projection.getDueTodayAssignedWorkOrders(),
                projection.getOverdueAssignedWorkOrders()
        );
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("No authenticated user found");
        }

        return authentication.getName();
    }

    private Long getCurrentTechnicianId() {
        String email = getCurrentUserEmail();

        Long userId = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found for email: " + email))
                .getId();

        return techRepo.findByUserId(userId)
                .map(technician -> technician.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated technician user is not linked to a technician record"
                ));
    }







    private List<DashboardSlaWorkOrderItem> toSlaItems(List<WorkOrderEntity> workOrders, LocalDate currentDate) {
        Map<Long, String> technicianNames = resolveTechnicianNames(workOrders);

        return workOrders.stream()
                .map(workOrder -> toSlaItem(workOrder, currentDate, technicianNames))
                .toList();
    }

    private DashboardSlaWorkOrderItem toSlaItem(
            WorkOrderEntity workOrder,
            LocalDate currentDate,
            Map<Long, String> technicianNames
    ) {
        Long workOrderId = workOrder.getId();
        String workOrderRef = workOrderId != null ? "WO-" + workOrderId : "Work order";

        long daysLate = 0;
        if (workOrder.getScheduledDate() != null && workOrder.getScheduledDate().isBefore(currentDate)) {
            daysLate = currentDate.toEpochDay() - workOrder.getScheduledDate().toEpochDay();
        }

        Long assignedTechId = workOrder.getAssignedTechId();
        String assignedTechName = technicianNames.get(assignedTechId);

        return new DashboardSlaWorkOrderItem(
                workOrderId,
                workOrderRef,
                abbreviate(workOrder.getDescription(), 60),
                workOrder.getClientName(),
                workOrder.getScheduledDate(),
                workOrder.getStatus(),
                workOrder.getPriority(),
                assignedTechId,
                assignedTechName,
                daysLate
        );
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "No description";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }

    private Map<Long, String> resolveTechnicianNames(List<WorkOrderEntity> workOrders) {
        List<Long> technicianIds = workOrders.stream()
                .map(WorkOrderEntity::getAssignedTechId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (technicianIds.isEmpty()) {
            return Map.of();
        }

        return techRepo.findAllById(technicianIds).stream()
                .collect(Collectors.toMap(
                        tech -> tech.getId(),
                        tech -> {
                            String fullName = tech.getFullName();
                            if (fullName != null && !fullName.isBlank()) {
                                return fullName;
                            }

                            if (tech.getEmail() != null && !tech.getEmail().isBlank()) {
                                return tech.getEmail().trim();
                            }

                            return "Technician #" + tech.getId();
                        }
                ));
    }
}
