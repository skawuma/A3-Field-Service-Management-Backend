package com.a3solutions.fsm.observability;

import com.a3solutions.fsm.technician.TechnicianRepository;
import com.a3solutions.fsm.workorder.WorkOrderRepository;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FsmOperationalMetrics {

    private static final List<WorkOrderStatus> TERMINAL_STATUSES = List.of(
            WorkOrderStatus.COMPLETED,
            WorkOrderStatus.CANCELLED
    );

    private final WorkOrderRepository workOrderRepository;
    private final TechnicianRepository technicianRepository;
    private final MeterRegistry meterRegistry;
    private final Map<WorkOrderStatus, AtomicLong> workOrderStatusCounts = new EnumMap<>(WorkOrderStatus.class);
    private final AtomicLong activeWorkOrders = new AtomicLong();
    private final AtomicLong overdueWorkOrders = new AtomicLong();
    private final AtomicLong dueTodayWorkOrders = new AtomicLong();
    private final AtomicLong unassignedActiveWorkOrders = new AtomicLong();
    private final AtomicLong assignedActiveWorkOrders = new AtomicLong();
    private final AtomicLong totalTechnicians = new AtomicLong();
    private final AtomicLong lastSlaSnapshotSize = new AtomicLong();

    public FsmOperationalMetrics(
            WorkOrderRepository workOrderRepository,
            TechnicianRepository technicianRepository,
            MeterRegistry meterRegistry
    ) {
        this.workOrderRepository = workOrderRepository;
        this.technicianRepository = technicianRepository;
        this.meterRegistry = meterRegistry;

        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            AtomicLong statusCount = new AtomicLong();
            workOrderStatusCounts.put(status, statusCount);
            Gauge.builder("a3.fsm.work.orders", statusCount, AtomicLong::get)
                    .description("Current work order count by lifecycle status.")
                    .tag("status", status.name())
                    .register(meterRegistry);
        }

        registerGauge("a3.fsm.work.orders.active", activeWorkOrders, "Current non-terminal work order count.");
        registerGauge("a3.fsm.work.orders.overdue", overdueWorkOrders, "Current active work orders past scheduled date.");
        registerGauge("a3.fsm.work.orders.due.today", dueTodayWorkOrders, "Current active work orders scheduled today.");
        registerGauge("a3.fsm.work.orders.unassigned.active", unassignedActiveWorkOrders, "Current active work orders without an assigned technician.");
        registerGauge("a3.fsm.work.orders.assigned.active", assignedActiveWorkOrders, "Current active work orders assigned to technicians.");
        registerGauge("a3.fsm.technicians.total", totalTechnicians, "Current technician record count.");
        registerGauge("a3.fsm.sla.monitor.last.snapshot.size", lastSlaSnapshotSize, "Most recent overdue work order snapshot size processed by the SLA monitor.");
    }

    @Scheduled(
            fixedDelayString = "${app.metrics.refresh-delay-ms:30000}",
            initialDelayString = "${app.metrics.refresh-initial-delay-ms:10000}"
    )
    public void refreshOperationalGauges() {
        LocalDate currentDate = LocalDate.now(ZoneId.systemDefault());

        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            workOrderStatusCounts.get(status).set(workOrderRepository.countByStatus(status));
        }

        activeWorkOrders.set(workOrderRepository.countByStatusNotIn(TERMINAL_STATUSES));
        overdueWorkOrders.set(workOrderRepository.countByScheduledDateBeforeAndStatusNotIn(currentDate, TERMINAL_STATUSES));
        dueTodayWorkOrders.set(workOrderRepository.countByScheduledDateAndStatusNotIn(currentDate, TERMINAL_STATUSES));
        unassignedActiveWorkOrders.set(workOrderRepository.countByAssignedTechIdIsNullAndStatusNotIn(TERMINAL_STATUSES));
        assignedActiveWorkOrders.set(workOrderRepository.countByAssignedTechIdIsNotNullAndStatusNotIn(TERMINAL_STATUSES));
        totalTechnicians.set(technicianRepository.count());
    }

    public void recordSlaMonitorRun(long overdueSnapshotSize) {
        lastSlaSnapshotSize.set(overdueSnapshotSize);
        meterRegistry.counter("a3.fsm.sla.monitor.runs").increment();
    }

    public void recordSlaBreachPublished() {
        meterRegistry.counter("a3.fsm.sla.breaches.published").increment();
    }

    public void recordRealtimeEventPublished(String destination) {
        Counter.builder("a3.fsm.realtime.events.published")
                .description("Realtime events published over dashboard, alert, and user notification channels.")
                .tag("destination", destination)
                .register(meterRegistry)
                .increment();
    }

    private void registerGauge(String name, AtomicLong value, String description) {
        Gauge.builder(name, value, AtomicLong::get)
                .description(description)
                .register(meterRegistry);
    }
}
