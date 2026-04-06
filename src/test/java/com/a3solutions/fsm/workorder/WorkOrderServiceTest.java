package com.a3solutions.fsm.workorder;

import com.a3solutions.fsm.exceptions.BusinessRuleException;
import com.a3solutions.fsm.storage.StorageService;
import com.a3solutions.fsm.technician.TechnicianEntity;
import com.a3solutions.fsm.technician.TechnicianRepository;
import com.a3solutions.fsm.workordercompletion.WorkOrderCompletionEntity;
import com.a3solutions.fsm.workordercompletion.WorkOrderCompletionRequest;
import com.a3solutions.fsm.workordercompletion.WorkOrderCompletionRepository;
import com.a3solutions.fsm.workordercompletion.WorkOrderCompletionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        assertEquals("Taylor Lane", result.assignedTechnicianName());

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
    void createLogsCreatedEvent() {
        WorkOrderCreateRequest request = new WorkOrderCreateRequest(
                "Acme",
                "123 Main",
                "New install",
                null,
                null,
                "MEDIUM",
                null
        );

        WorkOrderEntity savedEntity = WorkOrderEntity.builder()
                .id(10L)
                .clientName("Acme")
                .address("123 Main")
                .description("New install")
                .priority("MEDIUM")
                .status(WorkOrderStatus.OPEN)
                .build();

        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("dispatch@a3.com");
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        when(workOrderRepository.save(any(WorkOrderEntity.class))).thenReturn(savedEntity);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            WorkOrderDto result = workOrderService.create(request);

            assertEquals(10L, result.id());
            assertEquals(WorkOrderStatus.OPEN, result.status());
        }

        verify(workOrderEventService).logCreated(savedEntity, "dispatch@a3.com");
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
    void startWorkOrderRejectsCancelledWorkOrder() {
        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .id(12L)
                .clientName("Beta")
                .address("456 Oak")
                .description("Repair sensor")
                .assignedTechId(7L)
                .status(WorkOrderStatus.CANCELLED)
                .build();

        TechnicianEntity technician = TechnicianEntity.builder()
                .id(7L)
                .userId(41L)
                .build();

        when(technicianRepository.findByUserId(41L)).thenReturn(Optional.of(technician));
        when(workOrderRepository.findById(12L)).thenReturn(Optional.of(workOrder));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> workOrderService.startWorkOrder(12L, 41L)
        );

        assertEquals("Cancelled work orders cannot be started.", ex.getMessage());
        verify(workOrderRepository, never()).save(any());
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

        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("debs@a3fsm.com");

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            WorkOrderDto result = workOrderService.returnWorkOrderToOpen(13L, 41L, null);

            assertEquals(WorkOrderStatus.OPEN, workOrder.getStatus());
            assertNull(workOrder.getAssignedTechId());
            assertEquals(WorkOrderStatus.OPEN, result.status());
            assertNull(result.assignedTechId());
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
    void returnWorkOrderToOpenRejectsInProgressWorkOrders() {
        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .id(13L)
                .clientName("Gamma")
                .address("789 Pine")
                .description("Install panel")
                .assignedTechId(7L)
                .status(WorkOrderStatus.IN_PROGRESS)
                .build();

        TechnicianEntity technician = TechnicianEntity.builder()
                .id(7L)
                .userId(41L)
                .build();

        when(technicianRepository.findByUserId(41L)).thenReturn(Optional.of(technician));
        when(workOrderRepository.findById(13L)).thenReturn(Optional.of(workOrder));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> workOrderService.returnWorkOrderToOpen(13L, 41L, "delay")
        );

        assertEquals("Started work orders cannot be returned to OPEN from this screen.", ex.getMessage());
        verify(workOrderRepository, never()).save(any());
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

    @Test
    void reopenWorkOrderRejectsNonCompletedWorkOrders() {
        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .id(14L)
                .clientName("Delta")
                .address("900 Cedar")
                .description("Replace control board")
                .status(WorkOrderStatus.IN_PROGRESS)
                .build();

        when(workOrderRepository.findById(14L)).thenReturn(Optional.of(workOrder));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> workOrderService.reopenWorkOrder(14L, "needs review")
        );

        assertEquals("Only COMPLETED work orders can be reopened.", ex.getMessage());
        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void updateTechRejectsClosedWorkOrders() {
        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .id(15L)
                .clientName("Echo")
                .address("100 Lake")
                .description("Original notes")
                .assignedTechId(7L)
                .status(WorkOrderStatus.COMPLETED)
                .build();

        when(workOrderRepository.findById(15L)).thenReturn(Optional.of(workOrder));

        WorkOrderCreateRequest request = new WorkOrderCreateRequest(
                "Echo",
                "100 Lake",
                "Updated notes",
                7L,
                null,
                "HIGH",
                WorkOrderStatus.COMPLETED
        );

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> workOrderService.updateTech(15L, request, 7L)
        );

        assertEquals("Closed work orders cannot be updated by technicians.", ex.getMessage());
        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void updateTechRejectsStatusChangesFromNotesEndpoint() {
        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .id(16L)
                .clientName("Foxtrot")
                .address("200 River")
                .description("Original notes")
                .assignedTechId(7L)
                .status(WorkOrderStatus.IN_PROGRESS)
                .build();

        when(workOrderRepository.findById(16L)).thenReturn(Optional.of(workOrder));

        WorkOrderCreateRequest request = new WorkOrderCreateRequest(
                "Foxtrot",
                "200 River",
                "Updated notes",
                7L,
                null,
                "MEDIUM",
                WorkOrderStatus.COMPLETED
        );

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> workOrderService.updateTech(16L, request, 7L)
        );

        assertEquals("Technicians cannot change work order status from the notes update endpoint.", ex.getMessage());
        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void updateTechLogsNoteAddedWhenDescriptionChanges() {
        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .id(16L)
                .clientName("Foxtrot")
                .address("200 River")
                .description("Original notes")
                .assignedTechId(7L)
                .status(WorkOrderStatus.IN_PROGRESS)
                .build();

        when(workOrderRepository.findById(16L)).thenReturn(Optional.of(workOrder));
        when(workOrderRepository.save(workOrder)).thenReturn(workOrder);

        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("debs@a3fsm.com");
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        WorkOrderCreateRequest request = new WorkOrderCreateRequest(
                "Foxtrot",
                "200 River",
                "Updated notes",
                7L,
                null,
                "MEDIUM",
                WorkOrderStatus.IN_PROGRESS
        );

        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            WorkOrderDto result = workOrderService.updateTech(16L, request, 7L);

            assertEquals("Updated notes", result.description());
        }

        verify(workOrderEventService).logNoteAdded(workOrder, "Technician updated work notes.", "debs@a3fsm.com");
    }

    @Test
    void completeWorkOrderRejectsNonInProgressWorkOrders() {
        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .id(17L)
                .clientName("Golf")
                .address("300 Elm")
                .description("Replace keypad")
                .assignedTechId(7L)
                .status(WorkOrderStatus.COMPLETED)
                .build();

        TechnicianEntity technician = TechnicianEntity.builder()
                .id(7L)
                .userId(41L)
                .build();

        when(technicianRepository.findByUserId(41L)).thenReturn(Optional.of(technician));
        when(workOrderRepository.findById(17L)).thenReturn(Optional.of(workOrder));

        CompleteWorkOrderRequest request = new CompleteWorkOrderRequest(
                "data:image/png;base64,AAAA",
                "Signed off"
        );

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> workOrderService.completeWorkOrder(17L, request, 41L)
        );

        assertEquals("Only IN_PROGRESS work orders can be signed off.", ex.getMessage());
        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void completeWorkOrderRequiresStructuredCompletionReport() {
        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .id(18L)
                .clientName("Hotel")
                .address("400 Pine")
                .description("Repair reader")
                .assignedTechId(7L)
                .status(WorkOrderStatus.IN_PROGRESS)
                .build();

        TechnicianEntity technician = TechnicianEntity.builder()
                .id(7L)
                .userId(41L)
                .build();

        when(technicianRepository.findByUserId(41L)).thenReturn(Optional.of(technician));
        when(workOrderRepository.findById(18L)).thenReturn(Optional.of(workOrder));
        when(workOrderCompletionRepository.existsByWorkOrderId(18L)).thenReturn(false);

        CompleteWorkOrderRequest request = new CompleteWorkOrderRequest(
                "data:image/png;base64,AAAA",
                "Signed off"
        );

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> workOrderService.completeWorkOrder(18L, request, 41L)
        );

        assertEquals("Structured completion report is required before sign-off.", ex.getMessage());
        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void completeWorkOrderRequiresSignature() {
        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .id(19L)
                .clientName("India")
                .address("500 Ash")
                .description("Install printer")
                .assignedTechId(7L)
                .status(WorkOrderStatus.IN_PROGRESS)
                .build();

        TechnicianEntity technician = TechnicianEntity.builder()
                .id(7L)
                .userId(41L)
                .build();

        when(technicianRepository.findByUserId(41L)).thenReturn(Optional.of(technician));
        when(workOrderRepository.findById(19L)).thenReturn(Optional.of(workOrder));
        when(workOrderCompletionRepository.existsByWorkOrderId(19L)).thenReturn(true);

        CompleteWorkOrderRequest request = new CompleteWorkOrderRequest(
                "",
                "Signed off"
        );

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> workOrderService.completeWorkOrder(19L, request, 41L)
        );

        assertEquals("Signature is required.", ex.getMessage());
        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void completeWorkOrderLogsCompletionAndStatusChange() {
        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .id(22L)
                .clientName("Lima")
                .address("800 Oak")
                .description("Repair lock")
                .assignedTechId(7L)
                .status(WorkOrderStatus.IN_PROGRESS)
                .build();

        TechnicianEntity technician = TechnicianEntity.builder()
                .id(7L)
                .userId(41L)
                .build();

        when(technicianRepository.findByUserId(41L)).thenReturn(Optional.of(technician));
        when(workOrderRepository.findById(22L)).thenReturn(Optional.of(workOrder));
        when(workOrderCompletionRepository.existsByWorkOrderId(22L)).thenReturn(true);
        when(storageService.storeBytes(any(byte[].class), any(String.class), eq("image/png")))
                .thenReturn("signatures/workorder-22.png");
        when(workOrderRepository.save(workOrder)).thenReturn(workOrder);

        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("debs@a3fsm.com");
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        CompleteWorkOrderRequest request = new CompleteWorkOrderRequest(
                "data:image/png;base64,AAAA",
                "Signed off"
        );

        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            WorkOrderDto result = workOrderService.completeWorkOrder(22L, request, 41L);

            assertEquals(WorkOrderStatus.COMPLETED, result.status());
            assertEquals("signatures/workorder-22.png", result.signatureUrl());
        }

        verify(workOrderEventService).logCompleted(workOrder, "Signed off");
        verify(workOrderEventService).recordEvent(
                eq(workOrder),
                eq(WorkOrderEventType.STATUS_CHANGED),
                eq("Status changed to COMPLETED."),
                eq("IN_PROGRESS"),
                eq("COMPLETED"),
                eq("debs@a3fsm.com")
        );
    }

    @Test
    void submitStructuredCompletionReportRejectsDuplicateSubmission() {
        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .id(20L)
                .clientName("Juliet")
                .address("600 Maple")
                .description("Repair kiosk")
                .assignedTechId(7L)
                .status(WorkOrderStatus.IN_PROGRESS)
                .build();

        TechnicianEntity technician = TechnicianEntity.builder()
                .id(7L)
                .userId(41L)
                .build();

        WorkOrderCompletionRequest request = new WorkOrderCompletionRequest();
        request.setFaTag("FA-20");
        request.setIssueResolved(true);
        request.setReplacementNeeded(com.a3solutions.fsm.workordercompletion.ReplacementNeeded.NO);
        request.setReturnVisitRequired(false);
        request.setSummaryOfWork("Completed");

        when(technicianRepository.findByUserId(41L)).thenReturn(Optional.of(technician));
        when(workOrderRepository.findById(20L)).thenReturn(Optional.of(workOrder));
        when(workOrderCompletionRepository.existsByWorkOrderId(20L)).thenReturn(true);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> workOrderService.submitStructuredCompletionReport(20L, request, 41L)
        );

        assertEquals("Work order has already been completed", ex.getMessage());
        verify(workOrderCompletionRepository, never()).save(any());
    }

    @Test
    void submitStructuredCompletionReportKeepsWorkOrderInProgressUntilSignOff() {
        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .id(21L)
                .clientName("Kilo")
                .address("700 Birch")
                .description("Replace UPS")
                .assignedTechId(7L)
                .status(WorkOrderStatus.IN_PROGRESS)
                .build();

        TechnicianEntity technician = TechnicianEntity.builder()
                .id(7L)
                .userId(41L)
                .build();

        WorkOrderCompletionRequest request = new WorkOrderCompletionRequest();
        request.setFaTag("FA-21");
        request.setIssueResolved(true);
        request.setReplacementNeeded(com.a3solutions.fsm.workordercompletion.ReplacementNeeded.NO);
        request.setReturnVisitRequired(false);
        request.setSummaryOfWork("Replaced UPS battery");

        when(technicianRepository.findByUserId(41L)).thenReturn(Optional.of(technician));
        when(workOrderRepository.findById(21L)).thenReturn(Optional.of(workOrder));
        when(workOrderCompletionRepository.existsByWorkOrderId(21L)).thenReturn(false);
        when(workOrderCompletionRepository.save(any(WorkOrderCompletionEntity.class)))
                .thenAnswer(invocation -> {
                    WorkOrderCompletionEntity entity = invocation.getArgument(0);
                    entity.setWorkOrder(workOrder);
                    return entity;
                });

        WorkOrderCompletionResponse response = workOrderService.submitStructuredCompletionReport(21L, request, 41L);

        assertEquals(21L, response.getWorkOrderId());
        assertEquals(WorkOrderStatus.IN_PROGRESS, workOrder.getStatus());
        assertNull(workOrder.getCompletedAt());
        assertNull(workOrder.getCompletionNotes());
        verify(workOrderRepository, never()).save(any());
        verify(workOrderEventService, never()).logCompleted(any(), any());
    }
}
