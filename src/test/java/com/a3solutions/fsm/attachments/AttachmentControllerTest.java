package com.a3solutions.fsm.attachments;

import com.a3solutions.fsm.exceptions.BusinessRuleException;
import com.a3solutions.fsm.storage.StorageService;
import com.a3solutions.fsm.workorder.WorkOrderEntity;
import com.a3solutions.fsm.workorder.WorkOrderEventService;
import com.a3solutions.fsm.workorder.WorkOrderRepository;
import com.a3solutions.fsm.workorder.WorkOrderService;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentControllerTest {

    @Test
    void uploadRejectsClosedWorkOrders() {
        StorageService storageService = mock(StorageService.class);
        AttachmentRepository attachmentRepository = mock(AttachmentRepository.class);
        WorkOrderRepository workOrderRepository = mock(WorkOrderRepository.class);
        WorkOrderService workOrderService = mock(WorkOrderService.class);
        WorkOrderEventService workOrderEventService = mock(WorkOrderEventService.class);
        AttachmentController controller = new AttachmentController(
                storageService,
                attachmentRepository,
                workOrderRepository,
                workOrderService,
                workOrderEventService
        );

        WorkOrderEntity completedWorkOrder = WorkOrderEntity.builder()
                .id(33L)
                .status(WorkOrderStatus.COMPLETED)
                .build();

        when(workOrderRepository.findById(33L)).thenReturn(Optional.of(completedWorkOrder));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidence.txt",
                "text/plain",
                "test".getBytes()
        );

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> controller.upload(33L, file, null)
        );

        assertEquals("Closed work orders cannot accept new attachments.", ex.getMessage());
        verify(storageService, never()).store(any());
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void uploadAllowsOpenWorkOrders() {
        StorageService storageService = mock(StorageService.class);
        AttachmentRepository attachmentRepository = mock(AttachmentRepository.class);
        WorkOrderRepository workOrderRepository = mock(WorkOrderRepository.class);
        WorkOrderService workOrderService = mock(WorkOrderService.class);
        WorkOrderEventService workOrderEventService = mock(WorkOrderEventService.class);
        AttachmentController controller = new AttachmentController(
                storageService,
                attachmentRepository,
                workOrderRepository,
                workOrderService,
                workOrderEventService
        );

        WorkOrderEntity openWorkOrder = WorkOrderEntity.builder()
                .id(34L)
                .status(WorkOrderStatus.OPEN)
                .build();

        when(workOrderRepository.findById(34L)).thenReturn(Optional.of(openWorkOrder));
        when(storageService.store(any())).thenReturn("/files/evidence.txt");
        when(attachmentRepository.save(any(AttachmentEntity.class))).thenAnswer(invocation -> {
            AttachmentEntity entity = invocation.getArgument(0);
            entity.setId(99L);
            return entity;
        });

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidence.txt",
                "text/plain",
                "test".getBytes()
        );

        ResponseEntity<?> response = controller.upload(34L, file, null);
        Map<?, ?> body = (Map<?, ?>) response.getBody();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(99L, body.get("id"));
        assertEquals("/files/evidence.txt", body.get("url"));
        verify(storageService).store(file);
        verify(attachmentRepository).save(any(AttachmentEntity.class));
        verify(workOrderEventService).logAttachmentAdded(openWorkOrder, "evidence.txt", "SYSTEM");
    }
}
