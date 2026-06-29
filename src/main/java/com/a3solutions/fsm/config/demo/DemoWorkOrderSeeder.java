package com.a3solutions.fsm.config.demo;

import com.a3solutions.fsm.workorder.WorkOrderEntity;
import com.a3solutions.fsm.workorder.WorkOrderEventEntity;
import com.a3solutions.fsm.workorder.WorkOrderEventRepository;
import com.a3solutions.fsm.workorder.WorkOrderEventType;
import com.a3solutions.fsm.workorder.WorkOrderRepository;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import com.a3solutions.fsm.workordercompletion.ReplacementNeeded;
import com.a3solutions.fsm.workordercompletion.WorkOrderCompletionEntity;
import com.a3solutions.fsm.workordercompletion.WorkOrderCompletionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Component
@Profile("demo")
public class DemoWorkOrderSeeder {

    private static final String DEMO_ACTOR_ADMIN = "admin.demo@a3fsm.com";
    private static final String DEMO_ACTOR_DISPATCHER = "dispatcher.demo@a3fsm.com";
    private static final String DEMO_ACTOR_TECHNICIAN = "tech.demo@a3fsm.com";

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderEventRepository eventRepository;
    private final WorkOrderCompletionRepository completionRepository;

    public DemoWorkOrderSeeder(
            WorkOrderRepository workOrderRepository,
            WorkOrderEventRepository eventRepository,
            WorkOrderCompletionRepository completionRepository
    ) {
        this.workOrderRepository = workOrderRepository;
        this.eventRepository = eventRepository;
        this.completionRepository = completionRepository;
    }

    public int seed(
            DemoUserSeeder.DemoUsers users,
            DemoTechnicianSeeder.DemoTechnicians technicians
    ) {
        if (workOrderRepository.existsByClientNameAndDescription(
                "Northstar Legal Group",
                "Printer not responding"
        )) {
            return 0;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant now = Instant.now();

        WorkOrderEntity printer = createWorkOrder(
                "Northstar Legal Group",
                "75 Federal Street, Boston, MA",
                "Printer not responding",
                WorkOrderStatus.OPEN,
                null,
                today.plusDays(2),
                "HIGH",
                now.minus(8, ChronoUnit.HOURS)
        );
        recordCreated(printer);

        WorkOrderEntity scanner = createWorkOrder(
                "Lakeside Community Bank",
                "410 Riverside Avenue, Cambridge, MA",
                "Panini scanner replacement",
                WorkOrderStatus.IN_PROGRESS,
                technicians.james().getId(),
                today,
                "MEDIUM",
                now.minus(2, ChronoUnit.DAYS)
        );
        recordCreated(scanner);
        recordAssigned(scanner, technicians.james().getFullName());
        recordStarted(scanner);

        WorkOrderEntity networkJack = createWorkOrder(
                "Apex Business Center",
                "200 Seaport Boulevard, Boston, MA",
                "Network jack inactive",
                WorkOrderStatus.IN_PROGRESS,
                technicians.maria().getId(),
                today.minusDays(1),
                "HIGH",
                now.minus(3, ChronoUnit.DAYS)
        );
        recordCreated(networkJack);
        recordAssigned(networkJack, technicians.maria().getFullName());
        recordStarted(networkJack);

        WorkOrderEntity monitor = createCompletedWorkOrder(
                "Harborview Medical Clinic",
                "18 Harbor Way, Quincy, MA",
                "Monitor display issue",
                technicians.james().getId(),
                today.minusDays(4),
                "LOW",
                now.minus(6, ChronoUnit.DAYS),
                now.minus(4, ChronoUnit.DAYS),
                "Replaced the damaged display cable and verified dual-monitor output."
        );
        recordCompletedTimeline(monitor, technicians.james().getFullName());
        createCompletionReport(
                monitor,
                users.technician().getId(),
                "FA-10428",
                ReplacementNeeded.NO,
                "Replaced the failed DisplayPort cable, tested both displays, and confirmed stable output.",
                false,
                4
        );

        WorkOrderEntity teams = createCompletedWorkOrder(
                "Cedar Ridge Accounting",
                "92 Main Street, Waltham, MA",
                "Teams not launching",
                technicians.james().getId(),
                today.minusDays(2),
                "MEDIUM",
                now.minus(5, ChronoUnit.DAYS),
                now.minus(2, ChronoUnit.DAYS),
                "Cleared the local cache, repaired Microsoft 365, and validated sign-in."
        );
        recordCompletedTimeline(teams, technicians.james().getFullName());
        createCompletionReport(
                teams,
                users.technician().getId(),
                "FA-10431",
                ReplacementNeeded.NO,
                "Repaired the Microsoft 365 installation and confirmed Teams meetings launch normally.",
                false,
                2
        );

        WorkOrderEntity posTerminal = createWorkOrder(
                "Market Square Cafe",
                "11 Market Square, Somerville, MA",
                "POS terminal intermittently disconnecting",
                WorkOrderStatus.ASSIGNED,
                technicians.daniel().getId(),
                today.plusDays(1),
                "HIGH",
                now.minus(1, ChronoUnit.DAYS)
        );
        recordCreated(posTerminal);
        recordAssigned(posTerminal, technicians.daniel().getFullName());

        WorkOrderEntity labelPrinter = createWorkOrder(
                "BrightPath Logistics",
                "160 Commerce Drive, Chelsea, MA",
                "Warehouse label printer setup",
                WorkOrderStatus.ASSIGNED,
                technicians.maria().getId(),
                today.plusDays(3),
                "MEDIUM",
                now.minus(16, ChronoUnit.HOURS)
        );
        recordCreated(labelPrinter);
        recordAssigned(labelPrinter, technicians.maria().getFullName());

        WorkOrderEntity accessPoint = createWorkOrder(
                "Beacon Hill Dental",
                "27 Charles Street, Boston, MA",
                "Wireless access point offline",
                WorkOrderStatus.OPEN,
                null,
                today.minusDays(2),
                "HIGH",
                now.minus(4, ChronoUnit.DAYS)
        );
        recordCreated(accessPoint);

        return 8;
    }

    private WorkOrderEntity createWorkOrder(
            String clientName,
            String address,
            String description,
            WorkOrderStatus status,
            Long assignedTechId,
            LocalDate scheduledDate,
            String priority,
            Instant createdAt
    ) {
        return workOrderRepository.save(WorkOrderEntity.builder()
                .clientName(clientName)
                .address(address)
                .description(description)
                .status(status)
                .assignedTechId(assignedTechId)
                .scheduledDate(scheduledDate)
                .priority(priority)
                .createdAt(createdAt)
                .build());
    }

    private WorkOrderEntity createCompletedWorkOrder(
            String clientName,
            String address,
            String description,
            Long assignedTechId,
            LocalDate scheduledDate,
            String priority,
            Instant createdAt,
            Instant completedAt,
            String completionNotes
    ) {
        WorkOrderEntity workOrder = createWorkOrder(
                clientName,
                address,
                description,
                WorkOrderStatus.COMPLETED,
                assignedTechId,
                scheduledDate,
                priority,
                createdAt
        );
        workOrder.setCompletedAt(completedAt);
        workOrder.setCompletionNotes(completionNotes);
        return workOrderRepository.save(workOrder);
    }

    private void recordCreated(WorkOrderEntity workOrder) {
        recordEvent(
                workOrder,
                WorkOrderEventType.CREATED,
                "Work order created for " + workOrder.getClientName() + ".",
                null,
                WorkOrderStatus.OPEN.name(),
                DEMO_ACTOR_ADMIN
        );
    }

    private void recordAssigned(WorkOrderEntity workOrder, String technicianName) {
        recordEvent(
                workOrder,
                WorkOrderEventType.ASSIGNED_TECHNICIAN,
                "Assigned to " + technicianName + ".",
                null,
                technicianName,
                DEMO_ACTOR_DISPATCHER
        );
    }

    private void recordStarted(WorkOrderEntity workOrder) {
        recordEvent(
                workOrder,
                WorkOrderEventType.STARTED,
                "Technician started work on site.",
                WorkOrderStatus.ASSIGNED.name(),
                WorkOrderStatus.IN_PROGRESS.name(),
                DEMO_ACTOR_TECHNICIAN
        );
    }

    private void recordCompletedTimeline(WorkOrderEntity workOrder, String technicianName) {
        recordCreated(workOrder);
        recordAssigned(workOrder, technicianName);
        recordStarted(workOrder);
        recordEvent(
                workOrder,
                WorkOrderEventType.COMPLETED,
                "Work order completed and customer sign-off recorded.",
                WorkOrderStatus.IN_PROGRESS.name(),
                WorkOrderStatus.COMPLETED.name(),
                DEMO_ACTOR_TECHNICIAN
        );
    }

    private void recordEvent(
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
        eventRepository.save(event);
    }

    private void createCompletionReport(
            WorkOrderEntity workOrder,
            Long completedByUserId,
            String faTag,
            ReplacementNeeded replacementNeeded,
            String summary,
            boolean returnVisitRequired,
            long completedDaysAgo
    ) {
        WorkOrderCompletionEntity completion = new WorkOrderCompletionEntity();
        completion.setWorkOrder(workOrder);
        completion.setFaTag(faTag);
        completion.setIssueResolved(true);
        completion.setReplacementNeeded(replacementNeeded);
        completion.setReturnVisitRequired(returnVisitRequired);
        completion.setSummaryOfWork(summary);
        completion.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(completedDaysAgo));
        completion.setCompletedByUserId(completedByUserId);
        completionRepository.save(completion);
    }
}
