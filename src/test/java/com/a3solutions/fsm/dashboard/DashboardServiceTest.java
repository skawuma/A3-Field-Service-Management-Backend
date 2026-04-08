package com.a3solutions.fsm.dashboard;

import com.a3solutions.fsm.auth.UserRepository;
import com.a3solutions.fsm.technician.TechnicianRepository;
import com.a3solutions.fsm.workorder.WorkOrderEventRepository;
import com.a3solutions.fsm.workorder.WorkOrderRepository;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TechnicianRepository technicianRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private WorkOrderEventRepository workOrderEventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getSummaryIncludesSlaBucketsAndCompletionMetrics() {
        when(technicianRepository.count()).thenReturn(9L);
        when(workOrderRepository.count()).thenReturn(42L);
        when(workOrderRepository.countByStatus(WorkOrderStatus.OPEN)).thenReturn(11L);
        when(workOrderRepository.countByStatus(WorkOrderStatus.IN_PROGRESS)).thenReturn(7L);
        when(workOrderRepository.countByAssignedTechIdIsNull()).thenReturn(3L);
        when(workOrderRepository.countByScheduledDate(any(LocalDate.class))).thenReturn(6L);
        when(workOrderRepository.countByScheduledDateAndStatusNotIn(
                any(LocalDate.class),
                any(Collection.class)
        )).thenReturn(4L);
        when(workOrderRepository.countByScheduledDateBeforeAndStatusNotIn(
                any(LocalDate.class),
                any(Collection.class)
        )).thenReturn(2L);
        when(workOrderRepository.countByStatusAndCompletedAtBetween(
                eq(WorkOrderStatus.COMPLETED),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(5L);
        when(workOrderRepository.countByStatusAndPriorityIn(
                eq(WorkOrderStatus.OPEN),
                any(Collection.class)
        )).thenReturn(4L);

        DashboardSummary summary = dashboardService.getSummary();

        assertEquals(9L, summary.totalTechnicians());
        assertEquals(42L, summary.totalWorkOrders());
        assertEquals(11L, summary.openWorkOrders());
        assertEquals(7L, summary.inProgressWorkOrders());
        assertEquals(3L, summary.unassignedWorkOrders());
        assertEquals(6L, summary.scheduledToday());
        assertEquals(4L, summary.dueTodayWorkOrders());
        assertEquals(2L, summary.overdueWorkOrders());
        assertEquals(5L, summary.completedToday());
        assertEquals(4L, summary.highPriorityOpen());
    }

    @Test
    void getAnalyticsBuildsStableDashboardBuckets() {
        LocalDate today = LocalDate.now();

        when(workOrderRepository.countWorkOrdersByStatus()).thenReturn(List.of(
                bucket("OPEN", 5L),
                bucket("IN_PROGRESS", 3L),
                bucket("COMPLETED", 8L)
        ));
        when(workOrderRepository.countWorkOrdersByPriority()).thenReturn(List.of(
                bucket("LOW", 2L),
                bucket("HIGH", 6L),
                bucket("CRITICAL", 1L)
        ));
        when(workOrderRepository.countCompletionsByCompletedDate(any(Instant.class), any(Instant.class))).thenReturn(List.of(
                dateBucket(today.minusDays(1L), 4L),
                dateBucket(today, 2L)
        ));

        DashboardAnalytics analytics = dashboardService.getAnalytics();

        Map<String, Long> statusTotals = analytics.workOrdersByStatus().stream()
                .collect(Collectors.toMap(DashboardChartDatum::key, DashboardChartDatum::total));
        Map<String, Long> priorityTotals = analytics.workOrdersByPriority().stream()
                .collect(Collectors.toMap(DashboardChartDatum::key, DashboardChartDatum::total));
        Map<LocalDate, Long> trendTotals = analytics.completionTrend().stream()
                .collect(Collectors.toMap(DashboardTrendPoint::date, DashboardTrendPoint::total));

        assertEquals(5, analytics.workOrdersByStatus().size());
        assertEquals(5L, statusTotals.get("OPEN"));
        assertEquals(0L, statusTotals.get("ASSIGNED"));
        assertEquals(3L, statusTotals.get("IN_PROGRESS"));
        assertEquals(8L, statusTotals.get("COMPLETED"));
        assertEquals(0L, statusTotals.get("CANCELLED"));

        assertEquals(5, analytics.workOrdersByPriority().size());
        assertEquals(2L, priorityTotals.get("LOW"));
        assertEquals(0L, priorityTotals.get("MEDIUM"));
        assertEquals(6L, priorityTotals.get("HIGH"));
        assertEquals(1L, priorityTotals.get("CRITICAL"));
        assertEquals(0L, priorityTotals.get("UNSPECIFIED"));

        assertEquals(7, analytics.completionTrend().size());
        assertEquals(4L, trendTotals.get(today.minusDays(1L)));
        assertEquals(2L, trendTotals.get(today));
    }

    @Test
    void getTechnicianWorkloadBuildsDashboardFriendlyRows() {
        when(workOrderRepository.findTechnicianWorkloads(any(LocalDate.class), any(Collection.class))).thenReturn(List.of(
                technicianWorkload(1L, "Deborah Katimbo", 6L, 2L, 4L, 1L, 3L),
                technicianWorkload(2L, "Sam Okello", 3L, 1L, 2L, 0L, 1L)
        ));

        List<DashboardTechnicianWorkloadItem> workload = dashboardService.getTechnicianWorkload();

        assertEquals(2, workload.size());
        assertEquals("Deborah Katimbo", workload.getFirst().technicianName());
        assertEquals(6L, workload.getFirst().totalAssignedWorkOrders());
        assertEquals(2L, workload.getFirst().openAssignedWorkOrders());
        assertEquals(4L, workload.getFirst().inProgressAssignedWorkOrders());
        assertEquals(1L, workload.getFirst().dueTodayAssignedWorkOrders());
        assertEquals(3L, workload.getFirst().overdueAssignedWorkOrders());
    }

    private DashboardBucketProjection bucket(String key, long total) {
        return new DashboardBucketProjection() {
            @Override
            public String getBucketKey() {
                return key;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    private DashboardDateBucketProjection dateBucket(LocalDate bucketDate, long total) {
        return new DashboardDateBucketProjection() {
            @Override
            public LocalDate getBucketDate() {
                return bucketDate;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    private DashboardTechnicianWorkloadProjection technicianWorkload(
            Long technicianId,
            String technicianName,
            long totalAssignedWorkOrders,
            long openAssignedWorkOrders,
            long inProgressAssignedWorkOrders,
            long dueTodayAssignedWorkOrders,
            long overdueAssignedWorkOrders
    ) {
        return new DashboardTechnicianWorkloadProjection() {
            @Override
            public Long getTechnicianId() {
                return technicianId;
            }

            @Override
            public String getTechnicianName() {
                return technicianName;
            }

            @Override
            public long getTotalAssignedWorkOrders() {
                return totalAssignedWorkOrders;
            }

            @Override
            public long getOpenAssignedWorkOrders() {
                return openAssignedWorkOrders;
            }

            @Override
            public long getInProgressAssignedWorkOrders() {
                return inProgressAssignedWorkOrders;
            }

            @Override
            public long getDueTodayAssignedWorkOrders() {
                return dueTodayAssignedWorkOrders;
            }

            @Override
            public long getOverdueAssignedWorkOrders() {
                return overdueAssignedWorkOrders;
            }
        };
    }
}
