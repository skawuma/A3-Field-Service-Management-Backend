package com.a3solutions.fsm.timesheet;

import com.a3solutions.fsm.exceptions.BusinessRuleException;
import com.a3solutions.fsm.security.Role;
import com.a3solutions.fsm.technician.TechnicianEntity;
import com.a3solutions.fsm.technician.TechnicianRepository;
import com.a3solutions.fsm.workorder.WorkOrderEntity;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import com.a3solutions.fsm.workordercompletion.WorkOrderCompletionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.timesheet
 * @project A3 Field Service Management Backend
 * @date 07/03/26
 */
@ExtendWith(MockitoExtension.class)
class TimesheetServiceTest {

    @Mock
    private TimesheetRepository timesheetRepository;

    @Mock
    private TimesheetEntryRepository entryRepository;

    @Mock
    private TechnicianRepository technicianRepository;

    @Mock
    private WorkOrderCompletionRepository completionRepository;

    private TimesheetService timesheetService;

    @BeforeEach
    void setUp() {
        timesheetService = new TimesheetService(
                timesheetRepository,
                entryRepository,
                technicianRepository,
                completionRepository,
                "America/New_York"
        );
    }

    @Test
    void normalizeWeekStartUsesMondayThroughSundayRule() {
        assertEquals(LocalDate.of(2026, 6, 29),
                timesheetService.normalizeWeekStart(LocalDate.of(2026, 7, 3)));
        assertEquals(LocalDate.of(2026, 6, 29),
                timesheetService.normalizeWeekStart(LocalDate.of(2026, 7, 5)));
    }

    @Test
    void autoAddCompletedWorkOrderMapsPayrollFields() {
        TechnicianEntity technician = technician(7L, 41L, "Samuel", "Kawuma");
        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .id(22L)
                .assignedTechId(7L)
                .status(WorkOrderStatus.COMPLETED)
                .clientName("Quest Diagnostics")
                .address("200 Forest Street 3rd Floor Ste A, Marlborough, MA 01752")
                .arrivedAt(Instant.parse("2026-07-01T12:00:00Z"))
                .completedAt(Instant.parse("2026-07-01T20:00:00Z"))
                .completionNotes("Site sign-off complete")
                .build();
        TimesheetEntity weekly = timesheet(10L, 7L, TimesheetStatus.DRAFT);

        when(entryRepository.existsByWorkOrderId(22L)).thenReturn(false);
        when(timesheetRepository.findByTechnicianIdAndWeekStartDate(
                7L,
                LocalDate.of(2026, 6, 29)
        )).thenReturn(Optional.of(weekly));
        when(completionRepository.findByWorkOrderId(22L)).thenReturn(Optional.empty());
        when(entryRepository.save(any(TimesheetEntryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        timesheetService.autoAddCompletedWorkOrder(workOrder, technician);

        ArgumentCaptor<TimesheetEntryEntity> captor = ArgumentCaptor.forClass(TimesheetEntryEntity.class);
        verify(entryRepository).save(captor.capture());
        TimesheetEntryEntity entry = captor.getValue();
        assertEquals(10L, entry.getTimesheetId());
        assertEquals(LocalDate.of(2026, 7, 1), entry.getWorkDate());
        assertEquals("200 Forest Street 3rd Floor Ste A", entry.getSiteAddress());
        assertEquals("Marlborough", entry.getCity());
        assertEquals("MA", entry.getState());
        assertEquals("01752", entry.getZip());
        assertEquals(LocalTime.of(8, 0), entry.getOnsiteStartTime());
        assertEquals(LocalTime.of(16, 0), entry.getOffsiteEndTime());
        assertEquals("Site sign-off complete", entry.getComments());
        assertNull(entry.getMiles());
    }

    @Test
    void autoAddCompletedWorkOrderIsIdempotent() {
        TechnicianEntity technician = technician(7L, 41L, "Samuel", "Kawuma");
        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .id(22L)
                .assignedTechId(7L)
                .status(WorkOrderStatus.COMPLETED)
                .completedAt(Instant.parse("2026-07-01T20:00:00Z"))
                .build();
        when(entryRepository.existsByWorkOrderId(22L)).thenReturn(true);

        timesheetService.autoAddCompletedWorkOrder(workOrder, technician);

        verify(entryRepository, never()).save(any());
        verify(timesheetRepository, never()).save(any());
    }

    @Test
    void technicianCannotUpdateAnotherTechniciansEntry() {
        TimesheetEntryEntity entry = TimesheetEntryEntity.builder()
                .id(30L)
                .timesheetId(10L)
                .build();
        when(entryRepository.findById(30L)).thenReturn(Optional.of(entry));
        when(timesheetRepository.findById(10L)).thenReturn(Optional.of(
                timesheet(10L, 99L, TimesheetStatus.DRAFT)
        ));
        when(technicianRepository.findByUserId(41L)).thenReturn(Optional.of(
                technician(7L, 41L, "Samuel", "Kawuma")
        ));

        assertThrows(
                AccessDeniedException.class,
                () -> timesheetService.updateEntry(
                        30L,
                        new TimesheetEntryUpdateRequest(
                                BigDecimal.TEN,
                                LocalTime.of(8, 0),
                                null,
                                null,
                                LocalTime.of(16, 0),
                                "Completed"
                        ),
                        41L
                )
        );
        verify(entryRepository, never()).save(any());
    }

    @Test
    void technicianCannotViewAnotherTechniciansTimesheetButAdminCan() {
        TimesheetEntity anotherTechniciansSheet = timesheet(10L, 99L, TimesheetStatus.SUBMITTED);
        when(timesheetRepository.findById(10L)).thenReturn(Optional.of(anotherTechniciansSheet));
        when(technicianRepository.findByUserId(41L)).thenReturn(Optional.of(
                technician(7L, 41L, "Samuel", "Kawuma")
        ));
        when(entryRepository.findByTimesheetIdOrderByWorkDateAscIdAsc(10L)).thenReturn(List.of());

        assertThrows(
                AccessDeniedException.class,
                () -> timesheetService.getById(10L, 41L, Role.TECH)
        );
        assertEquals(10L, timesheetService.getById(10L, 1L, Role.ADMIN).id());
    }

    @Test
    void updateEntryRejectsInvalidTimeOrder() {
        TimesheetEntryEntity entry = TimesheetEntryEntity.builder()
                .id(30L)
                .timesheetId(10L)
                .build();
        when(entryRepository.findById(30L)).thenReturn(Optional.of(entry));
        when(timesheetRepository.findById(10L)).thenReturn(Optional.of(
                timesheet(10L, 7L, TimesheetStatus.DRAFT)
        ));
        when(technicianRepository.findByUserId(41L)).thenReturn(Optional.of(
                technician(7L, 41L, "Samuel", "Kawuma")
        ));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> timesheetService.updateEntry(
                        30L,
                        new TimesheetEntryUpdateRequest(
                                BigDecimal.TEN,
                                LocalTime.of(16, 0),
                                null,
                                null,
                                LocalTime.of(8, 0),
                                "Invalid shift"
                        ),
                        41L
                )
        );
        assertEquals("Offsite end time cannot be before onsite start time.", exception.getMessage());
        verify(entryRepository, never()).save(any());
    }

    @Test
    void technicianCanSubmitOwnValidDraftAndReviewerCanApproveIt() {
        TechnicianEntity technician = technician(7L, 41L, "Samuel", "Kawuma");
        TimesheetEntity draft = timesheet(10L, 7L, TimesheetStatus.DRAFT);
        TimesheetEntryEntity entry = TimesheetEntryEntity.builder()
                .id(30L)
                .timesheetId(10L)
                .workOrderId(22L)
                .workDate(LocalDate.of(2026, 7, 1))
                .clientName("Quest Diagnostics")
                .onsiteStartTime(LocalTime.of(8, 0))
                .offsiteEndTime(LocalTime.of(16, 0))
                .build();

        when(timesheetRepository.findById(10L)).thenReturn(Optional.of(draft));
        when(technicianRepository.findByUserId(41L)).thenReturn(Optional.of(technician));
        when(entryRepository.findByTimesheetIdOrderByWorkDateAscIdAsc(10L)).thenReturn(List.of(entry));
        when(timesheetRepository.save(any(TimesheetEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TimesheetResponse submitted = timesheetService.submit(
                10L,
                new TimesheetSubmitRequest("Samuel Kawuma"),
                41L
        );
        assertEquals(TimesheetStatus.SUBMITTED, submitted.status());
        assertEquals("Samuel Kawuma", submitted.technicianSignatureText());

        TimesheetResponse approved = timesheetService.approve(10L, Role.DISPATCH);
        assertEquals(TimesheetStatus.APPROVED, approved.status());
    }

    @Test
    void exportCsvIncludesContractTimesheetFields() {
        TimesheetEntity approved = timesheet(10L, 7L, TimesheetStatus.APPROVED);
        approved.setTechnicianSignatureText("Samuel Kawuma");
        TimesheetEntryEntity entry = TimesheetEntryEntity.builder()
                .id(30L)
                .timesheetId(10L)
                .workOrderId(22L)
                .workDate(LocalDate.of(2026, 7, 1))
                .clientName("Quest Diagnostics")
                .siteAddress("200 Forest Street")
                .city("Marlborough")
                .state("MA")
                .zip("01752")
                .miles(BigDecimal.valueOf(40))
                .onsiteStartTime(LocalTime.of(8, 0))
                .offsiteEndTime(LocalTime.of(16, 0))
                .comments("Completed, signed")
                .build();
        when(timesheetRepository.findById(10L)).thenReturn(Optional.of(approved));
        when(entryRepository.findByTimesheetIdOrderByWorkDateAscIdAsc(10L)).thenReturn(List.of(entry));

        String csv = new String(timesheetService.exportCsv(10L, 1L, Role.ADMIN), StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"Tech Name\",\"Samuel Kawuma\""));
        assertTrue(csv.contains("\"Date\",\"Miles\",\"Start Time Onsite\""));
        assertTrue(csv.contains("\"Completed, signed\""));
        assertTrue(csv.contains("\"Technician Signature\",\"Samuel Kawuma\""));
    }

    private TechnicianEntity technician(
            Long id,
            Long userId,
            String firstName,
            String lastName
    ) {
        return TechnicianEntity.builder()
                .id(id)
                .userId(userId)
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }

    private TimesheetEntity timesheet(Long id, Long technicianId, TimesheetStatus status) {
        return TimesheetEntity.builder()
                .id(id)
                .technicianId(technicianId)
                .technicianName("Samuel Kawuma")
                .weekStartDate(LocalDate.of(2026, 6, 29))
                .weekEndDate(LocalDate.of(2026, 7, 5))
                .status(status)
                .build();
    }
}
