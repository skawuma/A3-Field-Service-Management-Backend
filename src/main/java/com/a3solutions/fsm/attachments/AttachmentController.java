package com.a3solutions.fsm.attachments;

import com.a3solutions.fsm.storage.StorageService;
import jakarta.transaction.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    public AttachmentController(StorageService storageService,
                                AttachmentRepository attachmentRepo) {
        this.storageService = storageService;
        this.attachmentRepo = attachmentRepo;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    @Transactional
    public ResponseEntity<?> upload(
            @PathVariable Long workOrderId,
            @RequestParam("file") MultipartFile file
    ) {
        String url = storageService.store(file);

        var entity = AttachmentEntity.builder()
                .workOrderId(workOrderId)
                .filename(file.getOriginalFilename())
                .url(url)
                .sizeBytes(file.getSize())
                .build();

        attachmentRepo.save(entity);

        return ResponseEntity.ok(Map.of(
                "id", entity.getId(),
                "filename", entity.getFilename(),
                "url", entity.getUrl(),
                "size", entity.getSizeBytes()
        ));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<?> list(@PathVariable Long workOrderId) {
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
            @PathVariable Long attachmentId
    ) {
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