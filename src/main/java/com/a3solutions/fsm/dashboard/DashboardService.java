package com.a3solutions.fsm.dashboard;

import com.a3solutions.fsm.technician.TechnicianRepository;
import com.a3solutions.fsm.workorder.WorkOrderEntity;
import com.a3solutions.fsm.workorder.WorkOrderEventEntity;
import com.a3solutions.fsm.workorder.WorkOrderEventRepository;
import com.a3solutions.fsm.workorder.WorkOrderEventType;
import com.a3solutions.fsm.workorder.WorkOrderRepository;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.dashboard
 * @project A3 Field Service Management Backend
 * @date 11/19/25
 */
@Service
public class DashboardService {
    private final TechnicianRepository techRepo;
    private final WorkOrderRepository woRepo;
    private final WorkOrderEventRepository workOrderEventRepository;

    public DashboardService(
            TechnicianRepository techRepo,
            WorkOrderRepository woRepo,
            WorkOrderEventRepository workOrderEventRepository
    ) {
        this.techRepo = techRepo;
        this.woRepo = woRepo;
        this.workOrderEventRepository = workOrderEventRepository;
    }

    public DashboardSummary getSummary() {
        long totalTechs = techRepo.count();
        long totalWO = woRepo.count();
        long open = woRepo.countByStatus(WorkOrderStatus.OPEN);
        long inProgress = woRepo.countByStatus(WorkOrderStatus.IN_PROGRESS);
        long unassigned = woRepo.countByAssignedTechIdIsNull();
        long today = woRepo.countByScheduledDate(LocalDate.now());

        ZoneId zone = ZoneId.systemDefault();
        LocalDate currentDate = LocalDate.now(zone);
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
                today,
                completedToday,
                highPriorityOpen
        );
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
}
