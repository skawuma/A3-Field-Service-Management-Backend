package com.a3solutions.fsm.realtime;

import com.a3solutions.fsm.observability.FsmOperationalMetrics;
import com.a3solutions.fsm.workorder.WorkOrderEntity;
import com.a3solutions.fsm.workorder.WorkOrderRepository;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import com.a3solutions.fsm.workorder.WorkOrderEventService;
import com.a3solutions.fsm.workorder.WorkOrderEventType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.realtime
 * @project A3 Field Service Management Backend
 * @date 4/14/26
 */

/**
 * Sprint 11 execution-SLA monitor. Scheduled calendar dates remain a planning
 * concern; this monitor uses the policy-selected execution deadline.
 */
@Component
public class SlaRealtimeMonitor {

    private static final List<WorkOrderStatus> ACTIVE_SLA_STATUSES = List.of(
            WorkOrderStatus.EN_ROUTE,
            WorkOrderStatus.ARRIVED,
            WorkOrderStatus.WORK_STARTED,
            WorkOrderStatus.IN_PROGRESS
    );

    private final WorkOrderRepository workOrderRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final FsmOperationalMetrics metrics;
    private final WorkOrderEventService eventService;

    public SlaRealtimeMonitor(
            WorkOrderRepository workOrderRepository,
            RealtimeEventPublisher realtimeEventPublisher,
            FsmOperationalMetrics metrics,
            WorkOrderEventService eventService
    ) {
        this.workOrderRepository = workOrderRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.metrics = metrics;
        this.eventService = eventService;
    }

    @Scheduled(
            fixedDelayString = "${app.realtime.sla-monitor-delay-ms:60000}",
            initialDelayString = "${app.realtime.sla-monitor-initial-delay-ms:15000}"
    )
    @Transactional
    public void publishNewSlaBreaches() {
        Instant now = Instant.now();
        List<WorkOrderEntity> active = workOrderRepository
                .findBySlaDueAtIsNotNullAndStatusNotIn(List.of(WorkOrderStatus.COMPLETED, WorkOrderStatus.CANCELLED))
                .stream()
                .filter(wo -> ACTIVE_SLA_STATUSES.contains(wo.getStatus()))
                .toList();

        for (WorkOrderEntity workOrder : active) {
            if (now.isAfter(workOrder.getSlaDueAt()) && !Boolean.TRUE.equals(workOrder.getSlaBreached())) {
                workOrder.setSlaBreached(true);
                workOrder.setBreachMinutes((int) Math.max(0,
                        java.time.Duration.between(workOrder.getSlaDueAt(), now).toMinutes()));
                workOrderRepository.save(workOrder);
                eventService.recordEvent(workOrder, WorkOrderEventType.SLA_BREACHED,
                        "Execution SLA breached.", workOrder.getSlaDueAt().toString(), now.toString(), "SYSTEM");
            }
            realtimeEventPublisher.publishExecutionSlaState(workOrder, now);
        }
        metrics.recordSlaMonitorRun(active.size());
    }
}
