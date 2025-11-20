package com.a3solutions.fsm.attachments;

import com.a3solutions.fsm.storage.StorageService;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
                "url", entity.getUrl()
        ));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<?> list(@PathVariable Long workOrderId) {
        return ResponseEntity.ok(attachmentRepo.findByWorkOrderId(workOrderId));
    }
}
