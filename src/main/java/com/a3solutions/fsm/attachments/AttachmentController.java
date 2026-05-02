package com.a3solutions.fsm.attachments;

import com.a3solutions.fsm.auth.UserDetailsImpl;
import com.a3solutions.fsm.common.MessageResponse;
import com.a3solutions.fsm.exceptions.ApiErrorResponse;
import com.a3solutions.fsm.exceptions.BadRequestException;
import com.a3solutions.fsm.exceptions.BusinessRuleException;
import com.a3solutions.fsm.exceptions.NotFoundException;
import com.a3solutions.fsm.security.Role;
import com.a3solutions.fsm.storage.StorageService;
import com.a3solutions.fsm.workorder.WorkOrderEntity;
import com.a3solutions.fsm.workorder.WorkOrderEventService;
import com.a3solutions.fsm.workorder.WorkOrderRepository;
import com.a3solutions.fsm.workorder.WorkOrderService;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLConnection;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.attachments
 * @project A3 Field Service Management Backend
 * @date 11/19/25
 */
@RestController
@RequestMapping("/api/workorders/{workOrderId}/attachments")
@Tag(
        name = "Attachments",
        description = "Attachment upload, listing, download, and deletion for work orders."
)
@SecurityRequirement(name = "bearerAuth")
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

    @Operation(
            summary = "Upload an attachment",
            description = "Uploads a file to a work order. TECH users may only upload files for work orders assigned to them. COMPLETED and CANCELLED work orders are read-only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attachment uploaded", content = @Content(schema = @Schema(implementation = AttachmentResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller cannot upload to this work order", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Work order not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Closed work orders cannot accept attachments", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "File exceeds upload size limit", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    @Transactional
    public ResponseEntity<AttachmentResponse> upload(
            @Parameter(description = "Work order identifier", example = "42") @PathVariable Long workOrderId,
            @Parameter(
                    description = "Binary file to attach to the work order.",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    )
            )
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) {
        ensureTechCanAccess(auth, workOrderId);

        WorkOrderEntity workOrder = getWorkOrderOrThrow(workOrderId);

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
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .uploadedBy(actor)
                .build();

        attachmentRepo.save(entity);
        workOrderEventService.logAttachmentAdded(workOrder, entity.getFilename(), actor);

        return ResponseEntity.ok(toResponse(entity));
    }

    @Operation(
            summary = "List work-order attachments",
            description = "Returns attachment metadata for a work order. TECH users may only view attachments for work orders assigned to them."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attachments returned", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AttachmentResponse.class)))),
            @ApiResponse(responseCode = "403", description = "Caller cannot view attachments for this work order", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<java.util.List<AttachmentResponse>> list(
            @Parameter(description = "Work order identifier", example = "42") @PathVariable Long workOrderId,
            Authentication auth
    ) {
        ensureTechCanAccess(auth, workOrderId);

        return ResponseEntity.ok(
                attachmentRepo.findByWorkOrderId(workOrderId)
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    @Operation(
            summary = "Download an attachment",
            description = "Streams the stored file for a work order attachment. TECH users may only download attachments for work orders assigned to them."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attachment content streamed"),
            @ApiResponse(responseCode = "403", description = "Caller cannot download this attachment", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Attachment not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Attachment does not belong to the target work order", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{attachmentId}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH','TECH')")
    public ResponseEntity<Resource> download(
            @Parameter(description = "Work order identifier", example = "42") @PathVariable Long workOrderId,
            @Parameter(description = "Attachment identifier", example = "15") @PathVariable Long attachmentId,
            Authentication auth
    ) {
        ensureTechCanAccess(auth, workOrderId);

        var attachment = getAttachmentOrThrow(workOrderId, attachmentId);

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

    private String resolveContentType(AttachmentEntity attachment) {
        if (attachment.getContentType() != null && !attachment.getContentType().isBlank()) {
            return attachment.getContentType();
        }

        String guessed = URLConnection.guessContentTypeFromName(attachment.getFilename());
        return guessed != null ? guessed : "application/octet-stream";
    }

    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCH')")
    @Transactional
    @Operation(
            summary = "Delete an attachment",
            description = "Deletes an attachment from a work order. Restricted to ADMIN and DISPATCH users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attachment deleted", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Attachment not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Attachment does not belong to the target work order", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<MessageResponse> delete(
            @Parameter(description = "Work order identifier", example = "42") @PathVariable Long workOrderId,
            @Parameter(description = "Attachment identifier", example = "15") @PathVariable Long attachmentId
    ) {
        var attachment = getAttachmentOrThrow(workOrderId, attachmentId);

        storageService.delete(attachment.getUrl());
        attachmentRepo.delete(attachment);

        return ResponseEntity.ok(new MessageResponse("Attachment deleted successfully"));
    }

    private void ensureTechCanAccess(Authentication auth, Long workOrderId) {
        if (isUnauthorizedTech(auth, workOrderId)) {
            throw new AccessDeniedException("TECH can only access attachments for assigned work orders.");
        }
    }

    private WorkOrderEntity getWorkOrderOrThrow(Long workOrderId) {
        return workOrderRepo.findById(workOrderId)
                .orElseThrow(() -> new NotFoundException("Work order not found: " + workOrderId));
    }

    private AttachmentEntity getAttachmentOrThrow(Long workOrderId, Long attachmentId) {
        AttachmentEntity attachment = attachmentRepo.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("Attachment not found: " + attachmentId));

        if (!attachment.getWorkOrderId().equals(workOrderId)) {
            throw new BadRequestException("Attachment does not belong to work order: " + workOrderId);
        }

        return attachment;
    }

    private AttachmentResponse toResponse(AttachmentEntity attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getFilename(),
                attachment.getUrl(),
                resolveContentType(attachment),
                attachment.getSizeBytes(),
                attachment.getUploadedAt(),
                attachment.getUploadedBy()
        );
    }
}
