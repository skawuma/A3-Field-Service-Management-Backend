package com.a3solutions.fsm.workorder;

import com.a3solutions.fsm.storage.StorageService;
import com.a3solutions.fsm.technician.TechnicianEntity;
import com.a3solutions.fsm.technician.TechnicianRepository;
import com.a3solutions.fsm.workordercompletion.WorkOrderCompletionEntity;
import com.a3solutions.fsm.workordercompletion.WorkOrderCompletionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.MockedStatic;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private TechnicianRepository technicianRepository;

    @Mock
    private WorkOrderEventService workOrderEventService;

    @Mock
    private StorageService storageService;

    @Mock
    private WorkOrderCompletionRepository workOrderCompletionRepository;

    @InjectMocks
    private WorkOrderService workOrderService;

    @Test
    void assignTechnicianMovesOpenWorkOrderToAssigned() {
        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .id(11L)
                .clientName("Acme")
                .address("123 Main")
                .description("Install access point")
                .status(WorkOrderStatus.OPEN)
                .build();

        TechnicianEntity technician = TechnicianEntity.builder()
                .id(7L)
                .firstName("Taylor")
                .lastName("Lane")
                .userId(41L)
                .build();

        when(workOrderRepository.findById(11L)).thenReturn(Optional.of(workOrder));
        when(technicianRepository.findById(7L)).thenReturn(Optional.of(technician));
        when(workOrderRepository.save(workOrder)).thenReturn(workOrder);

        WorkOrderDto result = workOrderService.assignTechnician(
                11L,
                new AssignTechnicianRequest(7L),
                "dispatch@a3.com"
        );

        assertEquals(WorkOrderStatus.ASSIGNED, workOrder.getStatus());
        assertEquals(7L, workOrder.getAssignedTechId());
        assertEquals(WorkOrderStatus.ASSIGNED, result.status());
        assertEquals(7L, result.assignedTechId());

        verify(workOrderEventService).recordEvent(
                eq(workOrder),
                eq(WorkOrderEventType.ASSIGNED_TECHNICIAN),
                eq("Technician Taylor Lane was assigned to this work order."),
                eq((String) null),
                eq("7"),
                eq("dispatch@a3.com")
        );

        verify(workOrderEventService).recordEvent(
                eq(workOrder),
                eq(WorkOrderEventType.STATUS_CHANGED),
                eq("Status changed to ASSIGNED."),
                eq("OPEN"),
                eq("ASSIGNED"),
                eq("dispatch@a3.com")
        );
    }

    @Test
    void startWorkOrderMovesAssignedWorkOrderToInProgressForAssignedTech() {
        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .id(12L)
                .clientName("Beta")
                .address("456 Oak")
                .description("Repair sensor")
                .assignedTechId(7L)
                .status(WorkOrderStatus.ASSIGNED)
                .build();

        TechnicianEntity technician = TechnicianEntity.builder()
                .id(7L)
                .firstName("Taylor")
                .lastName("Lane")
                .userId(41L)
                .build();

        when(technicianRepository.findByUserId(41L)).thenReturn(Optional.of(technician));
        when(workOrderRepository.findById(12L)).thenReturn(Optional.of(workOrder));
        when(workOrderRepository.save(workOrder)).thenReturn(workOrder);

        WorkOrderDto result = workOrderService.startWorkOrder(12L, 41L);

        assertEquals(WorkOrderStatus.IN_PROGRESS, workOrder.getStatus());
        assertEquals(WorkOrderStatus.IN_PROGRESS, result.status());

        ArgumentCaptor<WorkOrderStatus> previousStatusCaptor = ArgumentCaptor.forClass(WorkOrderStatus.class);
        verify(workOrderEventService).logStarted(eq(workOrder), previousStatusCaptor.capture());
        assertSame(WorkOrderStatus.ASSIGNED, previousStatusCaptor.getValue());
    }

    @Test
    void returnWorkOrderToOpenClearsAssignmentForReassignment() {
        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .id(13L)
                .clientName("Gamma")
                .address("789 Pine")
                .description("Install panel")
                .assignedTechId(7L)
                .status(WorkOrderStatus.ASSIGNED)
                .build();

        TechnicianEntity technician = TechnicianEntity.builder()
                .id(7L)
                .firstName("Deborah")
                .lastName("Katimbo")
                .userId(41L)
                .build();

        when(technicianRepository.findByUserId(41L)).thenReturn(Optional.of(technician));
        when(workOrderRepository.findById(13L)).thenReturn(Optional.of(workOrder));
        when(workOrderRepository.save(workOrder)).thenReturn(workOrder);

        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("debs@a3fsm.com");
        SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            WorkOrderDto result = workOrderService.returnWorkOrderToOpen(13L, 41L);

            assertEquals(WorkOrderStatus.OPEN, workOrder.getStatus());
            assertEquals(null, workOrder.getAssignedTechId());
            assertEquals(WorkOrderStatus.OPEN, result.status());
            assertEquals(null, result.assignedTechId());
        }

        verify(workOrderEventService).recordEvent(
                eq(workOrder),
                eq(WorkOrderEventType.UNASSIGNED_TECHNICIAN),
                eq("Technician Deborah Katimbo released this work order so it can be reassigned."),
                eq("7"),
                eq((String) null),
                eq("debs@a3fsm.com")
        );

        verify(workOrderEventService).recordEvent(
                eq(workOrder),
                eq(WorkOrderEventType.STATUS_CHANGED),
                eq("Status changed to OPEN."),
                eq("ASSIGNED"),
                eq("OPEN"),
                eq("debs@a3fsm.com")
        );
    }

    @Test
    void reopenWorkOrderResetsCompletedStateAndDeletesCompletionReport() {
        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .id(14L)
                .clientName("Delta")
                .address("900 Cedar")
                .description("Replace control board")
                .assignedTechId(7L)
                .status(WorkOrderStatus.COMPLETED)
                .signatureUrl("signatures/workorder-14.png")
                .completionNotes("Completed on first visit")
                .completedAt(Instant.parse("2026-04-04T12:00:00Z"))
                .build();

        WorkOrderCompletionEntity completion = new WorkOrderCompletionEntity();
        completion.setWorkOrder(workOrder);

        when(workOrderRepository.findById(14L)).thenReturn(Optional.of(workOrder));
        when(workOrderCompletionRepository.findByWorkOrderId(14L)).thenReturn(Optional.of(completion));
        when(workOrderRepository.save(workOrder)).thenReturn(workOrder);

        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin@a3fsm.com");
        SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            WorkOrderDto result = workOrderService.reopenWorkOrder(14L, "needs new work");

            assertEquals(WorkOrderStatus.OPEN, workOrder.getStatus());
            assertEquals(WorkOrderStatus.OPEN, result.status());
            assertEquals(null, workOrder.getAssignedTechId());
            assertEquals(null, result.assignedTechId());
            assertEquals(null, workOrder.getCompletedAt());
            assertEquals(null, workOrder.getCompletionNotes());
            assertEquals(null, workOrder.getSignatureUrl());
        }

        verify(storageService).delete("signatures/workorder-14.png");
        verify(workOrderCompletionRepository).delete(completion);
        verify(workOrderEventService).logReopened(workOrder, "needs new work");
        verify(workOrderEventService).recordEvent(
                eq(workOrder),
                eq(WorkOrderEventType.STATUS_CHANGED),
                eq("Status changed to OPEN."),
                eq("COMPLETED"),
                eq("OPEN"),
                eq("admin@a3fsm.com")
        );
    }
}
