package com.a3solutions.fsm.attachments;

import com.a3solutions.fsm.auth.UserDetailsImpl;
import com.a3solutions.fsm.exceptions.BusinessRuleException;
import com.a3solutions.fsm.exceptions.NotFoundException;
import com.a3solutions.fsm.security.Role;
import com.a3solutions.fsm.storage.StorageService;
import com.a3solutions.fsm.workorder.WorkOrderEntity;
import com.a3solutions.fsm.workorder.WorkOrderEventService;
import com.a3solutions.fsm.workorder.WorkOrderRepository;
import com.a3solutions.fsm.workorder.WorkOrderService;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import jakarta.transaction.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.attachments
 * @project A3 Field Service Management Backend
 * @date 11/19/25
 */
@RestController
@RequestMapping("/api/workorders/{workOrderId}/attachments")
public class AttachmentController {

    private final StorageService storageService;
    private final AttachmentRepository attachmentRepo;
    private final WorkOrderRepository workOrderRepo;
    private final WorkOrderService workOrderService;
    private final WorkOrderEventService workOrderEventService;

    public AttachmentController(StorageService storageService,
                                AttachmentRepository attachmentRepo,
                                WorkOrderRepository workOrderRepo,
                                WorkOrderService workOrderService,
                                WorkOrderEventService workOrderEventService) {
        this.storageService = storageService;
        this.attachmentRepo = attachmentRepo;
        this.workOrderRepo = workOrderRepo;
        this.workOrderService = workOrderService;
        this.workOrderEventService = workOrderEventService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    @Transactional
    public ResponseEntity<?> upload(
            @PathVariable Long workOrderId,
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) {
        if (isUnauthorizedTech(auth, workOrderId)) {
            return ResponseEntity.status(403).body("TECH can only access attachments for assigned work orders.");
        }

        WorkOrderEntity workOrder = workOrderRepo.findById(workOrderId)
                .orElseThrow(() -> new NotFoundException("Work order not found: " + workOrderId));

        if (workOrder.getStatus() == WorkOrderStatus.COMPLETED ||
                workOrder.getStatus() == WorkOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Closed work orders cannot accept new attachments.");
        }

        String actor = resolveActor(auth);
        String url = storageService.store(file);

        var entity = AttachmentEntity.builder()
                .workOrderId(workOrderId)
                .filename(file.getOriginalFilename())
                .url(url)
                .sizeBytes(file.getSize())
                .uploadedBy(actor)
                .build();

        attachmentRepo.save(entity);
        workOrderEventService.logAttachmentAdded(workOrder, entity.getFilename(), actor);

        return ResponseEntity.ok(Map.of(
                "id", entity.getId(),
                "filename", entity.getFilename(),
                "url", entity.getUrl(),
                "size", entity.getSizeBytes()
        ));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<?> list(@PathVariable Long workOrderId, Authentication auth) {
        if (isUnauthorizedTech(auth, workOrderId)) {
            return ResponseEntity.status(403).body("TECH can only access attachments for assigned work orders.");
        }

        return ResponseEntity.ok(
                attachmentRepo.findByWorkOrderId(workOrderId)
                        .stream()
                        .map(att -> Map.of(
                                "id", att.getId(),
                                "filename", att.getFilename(),
                                "url", att.getUrl(),
                                "size", att.getSizeBytes()
                        ))
                        .toList()
        );
    }

    @GetMapping("/{attachmentId}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<Resource> download(
            @PathVariable Long workOrderId,
            @PathVariable Long attachmentId,
            Authentication auth
    ) {
        if (isUnauthorizedTech(auth, workOrderId)) {
            return ResponseEntity.status(403).build();
        }

        var attachment = attachmentRepo.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found: " + attachmentId));

        if (!attachment.getWorkOrderId().equals(workOrderId)) {
            throw new RuntimeException("Attachment does not belong to work order: " + workOrderId);
        }

        Resource resource = storageService.loadAsResource(attachment.getUrl());

        String contentType = "application/octet-stream";
        try {
            contentType = Files.probeContentType(Path.of(resource.getFile().getAbsolutePath()));
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getFilename() + "\"")
                .body(resource);
    }

    private boolean isUnauthorizedTech(Authentication auth, Long workOrderId) {
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl user)) {
            return false;
        }

        return user.getRole() == Role.TECH && !workOrderService.canTechAccessWorkOrder(workOrderId, user.getId());
    }

    private String resolveActor(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return "SYSTEM";
        }
        return auth.getName();
    }

    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    @Transactional
    public ResponseEntity<?> delete(
            @PathVariable Long workOrderId,
            @PathVariable Long attachmentId
    ) {
        var attachment = attachmentRepo.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found: " + attachmentId));

        if (!attachment.getWorkOrderId().equals(workOrderId)) {
            throw new RuntimeException("Attachment does not belong to work order: " + workOrderId);
        }

        storageService.delete(attachment.getUrl());
        attachmentRepo.delete(attachment);

        return ResponseEntity.ok(Map.of(
                "message", "Attachment deleted successfully"
        ));
    }
}
