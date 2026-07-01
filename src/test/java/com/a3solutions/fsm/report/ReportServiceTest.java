package com.a3solutions.fsm.report;

import com.a3solutions.fsm.exceptions.BadRequestException;
import com.a3solutions.fsm.technician.TechnicianEntity;
import com.a3solutions.fsm.technician.TechnicianRepository;
import com.a3solutions.fsm.workorder.WorkOrderEntity;
import com.a3solutions.fsm.workorder.WorkOrderRepository;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private TechnicianRepository technicianRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(workOrderRepository, technicianRepository);
    }

    @Test
    void buildsExportReadyOperationalReport() {
        Instant created = Instant.parse("2026-06-10T12:00:00Z");
        TechnicianEntity technician = TechnicianEntity.builder()
                .id(7L)
                .firstName("Aisha")
                .lastName("Morgan")
                .build();

        WorkOrderEntity completedWithinSla = workOrder(1L, created, WorkOrderStatus.COMPLETED, false);
        completedWithinSla.setAssignedTechId(7L);
        completedWithinSla.setAssignedAt(created.plusSeconds(15 * 60));
        completedWithinSla.setSlaClockStartedAt(created.plusSeconds(45 * 60));
        completedWithinSla.setCompletedAt(created.plusSeconds(105 * 60));
        completedWithinSla.setActualCompletionMinutes(60);

        WorkOrderEntity breached = workOrder(2L, created.plusSeconds(3600), WorkOrderStatus.COMPLETED, true);
        breached.setAssignedTechId(7L);
        breached.setSlaClockStartedAt(created.plusSeconds(2 * 3600));
        breached.setCompletedAt(created.plusSeconds(5 * 3600));
        breached.setActualCompletionMinutes(180);

        WorkOrderEntity active = workOrder(3L, created.plusSeconds(7200), WorkOrderStatus.OPEN, false);

        when(workOrderRepository.findAll()).thenReturn(List.of(completedWithinSla, breached, active));
        when(technicianRepository.findAll()).thenReturn(List.of(technician));

        OperationsReport report = reportService.getOperationsReport(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        assertThat(report.summary().totalWorkOrders()).isEqualTo(3);
        assertThat(report.summary().completedWorkOrders()).isEqualTo(2);
        assertThat(report.summary().activeWorkOrders()).isEqualTo(1);
        assertThat(report.summary().slaCompliancePercent()).isEqualTo(50.0);
        assertThat(report.technicianPerformance()).singleElement().satisfies(performance -> {
            assertThat(performance.technicianName()).isEqualTo("Aisha Morgan");
            assertThat(performance.averageCompletionMinutes()).isEqualTo(120);
        });
        assertThat(report.workOrders()).hasSize(3);
        assertThat(report.workOrders().get(1).slaOutcome()).isEqualTo("BREACHED");
    }

    @Test
    void rejectsInvalidOrExcessivePeriods() {
        assertThatThrownBy(() -> reportService.getOperationsReport(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 6, 1)
        )).isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> reportService.getOperationsReport(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 6, 30)
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    void ignoresCompletedWorkOrdersWithoutCompletionTimingData() {
        Instant created = Instant.parse("2026-06-10T12:00:00Z");
        WorkOrderEntity completedWithoutTiming = workOrder(4L, created, WorkOrderStatus.COMPLETED, false);
        completedWithoutTiming.setAssignedTechId(7L);

        when(workOrderRepository.findAll()).thenReturn(List.of(completedWithoutTiming));
        when(technicianRepository.findAll()).thenReturn(List.of());

        OperationsReport report = reportService.getOperationsReport(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        assertThat(report.technicianPerformance()).singleElement().satisfies(performance ->
                assertThat(performance.averageCompletionMinutes()).isNull());
    }

    private WorkOrderEntity workOrder(Long id, Instant createdAt, WorkOrderStatus status, boolean breached) {
        return WorkOrderEntity.builder()
                .id(id)
                .clientName("Client " + id)
                .description("Service request " + id)
                .status(status)
                .priority(id == 2 ? "HIGH" : "MEDIUM")
                .scheduledDate(LocalDate.of(2026, 6, 15))
                .slaBreached(breached)
                .createdAt(createdAt)
                .build();
    }
}
