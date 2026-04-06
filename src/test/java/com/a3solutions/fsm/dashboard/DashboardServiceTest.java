package com.a3solutions.fsm.dashboard;

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

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getSummaryIncludesCompletedTodayAndHighPriorityOpen() {
        when(technicianRepository.count()).thenReturn(9L);
        when(workOrderRepository.count()).thenReturn(42L);
        when(workOrderRepository.countByStatus(WorkOrderStatus.OPEN)).thenReturn(11L);
        when(workOrderRepository.countByStatus(WorkOrderStatus.IN_PROGRESS)).thenReturn(7L);
        when(workOrderRepository.countByAssignedTechIdIsNull()).thenReturn(3L);
        when(workOrderRepository.countByScheduledDate(any(LocalDate.class))).thenReturn(6L);
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
        assertEquals(5L, summary.completedToday());
        assertEquals(4L, summary.highPriorityOpen());
    }
}
