package com.a3solutions.fsm.dashboard;

import com.a3solutions.fsm.auth.UserEntity;
import com.a3solutions.fsm.auth.UserRepository;
import com.a3solutions.fsm.security.Role;
import com.a3solutions.fsm.technician.TechnicianEntity;
import com.a3solutions.fsm.technician.TechnicianRepository;
import com.a3solutions.fsm.workorder.WorkOrderEntity;
import com.a3solutions.fsm.workorder.WorkOrderEventEntity;
import com.a3solutions.fsm.workorder.WorkOrderEventType;
import com.a3solutions.fsm.workorder.WorkOrderEventRepository;
import com.a3solutions.fsm.workorder.WorkOrderRepository;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

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
        when(workOrderRepository.countByAssignedTechIdIsNotNullAndStatusNotIn(any(Collection.class))).thenReturn(17L);
        when(workOrderRepository.countByAssignedTechIdIsNotNullAndStatus(WorkOrderStatus.IN_PROGRESS)).thenReturn(6L);

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
        assertEquals(17L, summary.activeAssignedWorkOrders());
        assertEquals(6L, summary.assignedInProgressWorkOrders());
    }

    @Test
    void getSummaryReturnsTechnicianPersonalCountsForTechDashboard() {
        setAuthenticatedUser("debs@a3fsm.com", "TECH");

        when(userRepository.findByEmail("debs@a3fsm.com")).thenReturn(
                java.util.Optional.of(UserEntity.builder()
                        .id(77L)
                        .email("debs@a3fsm.com")
                        .role(Role.TECH)
                        .firstName("Deborah")
                        .lastName("Katimbo")
                        .password("secret")
                        .build())
        );
        when(technicianRepository.findByUserId(77L)).thenReturn(
                java.util.Optional.of(TechnicianEntity.builder()
                        .id(5L)
                        .userId(77L)
                        .firstName("Deborah")
                        .lastName("Katimbo")
                        .email("debs@a3fsm.com")
                        .build())
        );
        when(workOrderRepository.countByAssignedTechIdAndScheduledDateAndStatusNotIn(
                eq(5L),
                any(LocalDate.class),
                any(Collection.class)
        )).thenReturn(3L);
        when(workOrderRepository.countByAssignedTechIdAndScheduledDateBeforeAndStatusNotIn(
                eq(5L),
                any(LocalDate.class),
                any(Collection.class)
        )).thenReturn(2L);
        when(workOrderRepository.countByAssignedTechIdAndStatusNotIn(
                eq(5L),
                any(Collection.class)
        )).thenReturn(6L);
        when(workOrderRepository.countByAssignedTechIdAndStatus(5L, WorkOrderStatus.IN_PROGRESS)).thenReturn(4L);

        DashboardSummary summary = dashboardService.getSummary();

        assertEquals(3L, summary.dueTodayWorkOrders());
        assertEquals(2L, summary.overdueWorkOrders());
        assertEquals(6L, summary.activeAssignedWorkOrders());
        assertEquals(4L, summary.assignedInProgressWorkOrders());
        assertEquals(0L, summary.totalWorkOrders());
        assertEquals(0L, summary.completedToday());
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

    @Test
    void getRecentActivityReturnsTechnicianPersonalActivity() {
        setAuthenticatedUser("debs@a3fsm.com", "TECH");

        when(userRepository.findByEmail("debs@a3fsm.com")).thenReturn(
                java.util.Optional.of(UserEntity.builder()
                        .id(77L)
                        .email("debs@a3fsm.com")
                        .role(Role.TECH)
                        .firstName("Deborah")
                        .lastName("Katimbo")
                        .password("secret")
                        .build())
        );
        when(technicianRepository.findByUserId(77L)).thenReturn(
                java.util.Optional.of(TechnicianEntity.builder()
                        .id(5L)
                        .userId(77L)
                        .firstName("Deborah")
                        .lastName("Katimbo")
                        .email("debs@a3fsm.com")
                        .build())
        );

        WorkOrderEntity assignedToTech = WorkOrderEntity.builder()
                .id(101L)
                .assignedTechId(5L)
                .status(WorkOrderStatus.IN_PROGRESS)
                .build();
        WorkOrderEntity notAssignedToTech = WorkOrderEntity.builder()
                .id(202L)
                .assignedTechId(9L)
                .status(WorkOrderStatus.OPEN)
                .build();

        WorkOrderEventEntity ownAssignedEvent = event(assignedToTech, WorkOrderEventType.STARTED, "debs@a3fsm.com", "Started work");
        WorkOrderEventEntity ownActorEvent = event(notAssignedToTech, WorkOrderEventType.COMPLETED, "debs@a3fsm.com", "Completed work");
        WorkOrderEventEntity unrelatedEvent = event(notAssignedToTech, WorkOrderEventType.CREATED, "admin@a3fsm.com", "Created work");

        when(workOrderEventRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(List.of(
                ownAssignedEvent,
                ownActorEvent,
                unrelatedEvent
        ));

        List<DashboardRecentActivityItem> activity = dashboardService.getRecentActivity(8);

        assertEquals(2, activity.size());
        assertEquals(List.of(101L, 202L), activity.stream().map(DashboardRecentActivityItem::workOrderId).toList());
    }

    private void setAuthenticatedUser(String email, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email,
                        "n/a",
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                )
        );
    }

    private WorkOrderEventEntity event(
            WorkOrderEntity workOrder,
            WorkOrderEventType eventType,
            String actor,
            String message
    ) {
        WorkOrderEventEntity event = new WorkOrderEventEntity();
        event.setWorkOrder(workOrder);
        event.setEventType(eventType);
        event.setActor(actor);
        event.setMessage(message);
        return event;
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
