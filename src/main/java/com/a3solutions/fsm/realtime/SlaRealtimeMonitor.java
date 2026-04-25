package com.a3solutions.fsm.realtime;

import com.a3solutions.fsm.workorder.WorkOrderEntity;
import com.a3solutions.fsm.workorder.WorkOrderRepository;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.realtime
 * @project A3 Field Service Management Backend
 * @date 4/14/26
 */

/**
 * Lightweight Sprint 8 SLA breach publisher. This keeps alerting simple for
 * now and avoids introducing a persistent alerts table before it is needed.
 */
@Component
public class SlaRealtimeMonitor {

    private static final List<WorkOrderStatus> ACTIVE_SLA_STATUSES = List.of(
            WorkOrderStatus.OPEN,
            WorkOrderStatus.ASSIGNED,
            WorkOrderStatus.IN_PROGRESS
    );

    private final WorkOrderRepository workOrderRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final Set<Long> publishedOverdueWorkOrderIds = ConcurrentHashMap.newKeySet();

    public SlaRealtimeMonitor(
            WorkOrderRepository workOrderRepository,
            RealtimeEventPublisher realtimeEventPublisher
    ) {
        this.workOrderRepository = workOrderRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Scheduled(
            fixedDelayString = "${app.realtime.sla-monitor-delay-ms:60000}",
            initialDelayString = "${app.realtime.sla-monitor-initial-delay-ms:15000}"
    )
    public void publishNewSlaBreaches() {
        LocalDate currentDate = LocalDate.now(ZoneId.systemDefault());

        List<WorkOrderEntity> overdueWorkOrders =
                workOrderRepository.findByScheduledDateBeforeAndStatusInOrderByScheduledDateAscIdAsc(
                        currentDate,
                        ACTIVE_SLA_STATUSES,
                        Pageable.unpaged()
                );

        Set<Long> currentOverdueIds = overdueWorkOrders.stream()
                .map(WorkOrderEntity::getId)
                .collect(Collectors.toSet());

        publishedOverdueWorkOrderIds.retainAll(currentOverdueIds);

        for (WorkOrderEntity workOrder : overdueWorkOrders) {
            if (!publishedOverdueWorkOrderIds.add(workOrder.getId())) {
                continue;
            }

            long overdueDays = ChronoUnit.DAYS.between(workOrder.getScheduledDate(), currentDate);
            realtimeEventPublisher.publishSlaBreached(workOrder, overdueDays);
        }
    }
}
