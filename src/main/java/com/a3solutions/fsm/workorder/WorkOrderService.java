package com.a3solutions.fsm.workorder;

import com.a3solutions.fsm.common.PageResponse;
import com.a3solutions.fsm.exceptions.BusinessRuleException;
import com.a3solutions.fsm.exceptions.NotFoundException;
import com.a3solutions.fsm.security.Role;
import com.a3solutions.fsm.technician.TechnicianEntity;
import com.a3solutions.fsm.technician.TechnicianRepository;
import com.a3solutions.fsm.workordercompletion.WorkOrderCompletionEntity;
import com.a3solutions.fsm.workordercompletion.WorkOrderCompletionRepository;
import com.a3solutions.fsm.workordercompletion.WorkOrderCompletionRequest;
import com.a3solutions.fsm.workordercompletion.WorkOrderCompletionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import com.a3solutions.fsm.storage.StorageService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
@Service
public class WorkOrderService {
    private static final Logger log = LoggerFactory.getLogger(WorkOrderService.class);

    private final StorageService storageService;
    private final WorkOrderRepository repo;
    private final TechnicianRepository technicianRepo;
    private final WorkOrderEventService eventService;
    private final WorkOrderCompletionRepository workOrderCompletionRepository;

    public WorkOrderService(
            WorkOrderRepository repo,
            TechnicianRepository technicianRepo,
            WorkOrderEventService eventService,
            StorageService storageService,
            WorkOrderCompletionRepository workOrderCompletionRepository) {
        this.repo = repo;
        this.technicianRepo = technicianRepo;
        this.eventService = eventService;
        this.storageService = storageService;
        this.workOrderCompletionRepository = workOrderCompletionRepository;
    }

    // =====================================================================
    // GET PAGE
    // =====================================================================
    public PageResponse<WorkOrderDto> getPage(
            int page, int size,
            String search, String priority,
            String status, String sort,
            Long technicianId
    ) {
        Pageable pageable = buildPageable(page, size, sort);

        Specification<WorkOrderEntity> spec = Specification
                .where(WorkOrderSpecification.hasSearch(search))
                .and(WorkOrderSpecification.hasPriority(priority))
                .and(WorkOrderSpecification.hasStatus(status))
                .and(WorkOrderSpecification.assignedTo(technicianId));

        Page<WorkOrderEntity> results = repo.findAll(spec, pageable);

        return PageResponse.of(
                results.map(this::toDto).getContent(),
                results.getNumber(),
                results.getSize(),
                results.getTotalElements()
        );
    }

    // =====================================================================
    // GET BY ID  — REQUIRED
    // =====================================================================
    public WorkOrderDto getById(Long id) {
        var wo = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Work order not found: " + id));

        return toDto(wo);
    }

    // =====================================================================
    // CREATE
    // =====================================================================
    @Transactional
    public WorkOrderDto create(WorkOrderCreateRequest req) {
        var entity = WorkOrderEntity.builder()
                .clientName(req.clientName())
                .address(req.address())
                .description(req.description())
                .assignedTechId(req.assignedTechId())
                .scheduledDate(req.scheduledDate())
                .priority(req.priority())
                .status(req.status() != null ? req.status() : WorkOrderStatus.OPEN)
                .build();

        return toDto(repo.save(entity));
    }

    // =====================================================================
    // ASSIGN TECHNICIAN
    // =====================================================================
    @Transactional
    public WorkOrderDto assignTechnician(Long id, AssignTechnicianRequest req, String actor) {

        // 1. Fetch work order
        var wo = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Work order not found: " + id));

        // 2. Validate technician exists
        var tech = technicianRepo.findById(req.technicianId())
                .orElseThrow(() -> new NotFoundException("Technician not found: " + req.technicianId()));

        // Store old values (for event log)
        Long oldTechId = wo.getAssignedTechId();
        WorkOrderStatus oldStatus = wo.getStatus();

        // 3. Update work order
        wo.setAssignedTechId(req.technicianId());
        WorkOrderStatus nextStatus = oldStatus;
        if (oldStatus == null || oldStatus == WorkOrderStatus.OPEN || oldStatus == WorkOrderStatus.ASSIGNED) {
            nextStatus = WorkOrderStatus.ASSIGNED;
            wo.setStatus(nextStatus);
        }

        var saved = repo.save(wo);

        // 4. Record assignment event
        String technicianName = tech.getFullName()!= null ? tech.getFullName() : ("Tech#" + tech.getId());

        eventService.recordEvent(
                saved,
                WorkOrderEventType.ASSIGNED_TECHNICIAN,
                "Technician " + technicianName + " was assigned to this work order.",
                oldTechId == null ? null : oldTechId.toString(),
                tech.getId().toString(),
                actor
        );

        // 5. If status changed → record event
        if (oldStatus != nextStatus) {
            eventService.recordEvent(
                    saved,
                    WorkOrderEventType.STATUS_CHANGED,
                    "Status changed to " + nextStatus.name() + ".",
                    oldStatus == null ? null : oldStatus.name(),
                    nextStatus.name(),
                    actor
            );
        }

        // 6. Return updated DTO
        return toDto(saved);
    }


    // =====================================================================
    // ADMIN + DISPATCH FULL UPDATE
    // =====================================================================
    @Transactional
    public WorkOrderDto updateAdmin(Long id, WorkOrderCreateRequest req) {

        var existing = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Work order not found: " + id));

        existing.setClientName(req.clientName());
        existing.setAddress(req.address());
        existing.setDescription(req.description());
        existing.setAssignedTechId(req.assignedTechId());
        existing.setScheduledDate(req.scheduledDate());
        existing.setPriority(req.priority());
        existing.setStatus(req.status());

        return toDto(repo.save(existing));
    }

    // =====================================================================
    // TECH LIMITED UPDATE
    // =====================================================================
    @Transactional
    public WorkOrderDto updateTech(Long id, WorkOrderCreateRequest req, Long techId) {

        var existing = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Work order not found: " + id));

        if (!techId.equals(existing.getAssignedTechId())) {
            throw new NotFoundException("TECH is not assigned to this work order.");
        }

        // TECH may update only limited fields
        existing.setDescription(req.description());

        // TECH can only move IN_PROGRESS → COMPLETED
        if (req.status() == WorkOrderStatus.COMPLETED &&
                existing.getStatus() == WorkOrderStatus.IN_PROGRESS) {
            existing.setStatus(WorkOrderStatus.COMPLETED);
        }

        return toDto(repo.save(existing));
    }


    // =====================================================================
    // HELPERS FOR TECH USER
    // =====================================================================

    /** 🔥 Map logged-in userId → technicianId (or null if not mapped) */
    public Long findTechnicianIdForUser(Long userId) {
        return technicianRepo.findByUserId(userId)
                .map(TechnicianEntity::getId)
                .orElse(null);
    }

    /** 🔥 Check if a TECH user (by userId) is allowed to access this work order */
//    public boolean canTechAccessWorkOrder(Long workOrderId, Long userId) {
//        var techOpt = technicianRepo.findByUserId(userId);
//        if (techOpt.isEmpty()) {
//            return false;
//        }
//        Long technicianId = techOpt.get().getId();
//
//        return repo.findById(workOrderId)
//                .map(wo -> technicianId.equals(wo.getAssignedTechId()))
//                .orElse(false);
//    }

    public boolean canTechAccessWorkOrder(Long workOrderId, Long userId) {
        var techOpt = technicianRepo.findByUserId(userId);
        if (techOpt.isEmpty()) {
            return false;
        }

        Long technicianId = techOpt.get().getId();

        return repo.findById(workOrderId)
                .map(wo -> Objects.equals(wo.getAssignedTechId(), technicianId))
                .orElse(false);
    }

    /** 🔥 TECH update using userId */
    @Transactional
    public WorkOrderDto updateTechByUser(Long id, WorkOrderCreateRequest req, Long userId) {
        var techOpt = technicianRepo.findByUserId(userId);
        if (techOpt.isEmpty()) {
            throw new NotFoundException("No technician linked to this user.");
        }
        Long technicianId = techOpt.get().getId();
        return updateTech(id, req, technicianId);
    }



    // =====================================================================
    // HELPERS
    // =====================================================================

    public boolean isAssignedToTech(Long workOrderId, Long techId) {
        return repo.findById(workOrderId)
                .map(wo -> techId.equals(wo.getAssignedTechId()))
                .orElse(false);
    }

    private Pageable buildPageable(int page, int size, String sort) {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size, Sort.by("id").descending());
        }
        String[] parts = sort.split(",");
        String field = parts[0];
        String dir = parts.length > 1 ? parts[1] : "desc";
        return PageRequest.of(
                page, size,
                "asc".equalsIgnoreCase(dir)
                        ? Sort.by(field).ascending()
                        : Sort.by(field).descending()
        );
    }

    private WorkOrderDto toDto(WorkOrderEntity e) {
        return new WorkOrderDto(
                e.getId(),
                e.getClientName(),
                e.getAddress(),
                e.getDescription(),
                e.getStatus(),
                e.getAssignedTechId(),
                e.getScheduledDate(),
                e.getPriority(),
                e.getSignatureUrl(),
                e.getCompletionNotes(),
                e.getCompletedAt()
        );
    }


    @Transactional
    public WorkOrderDto completeWorkOrder(Long id, CompleteWorkOrderRequest req, Long userId) {

        Long technicianId = technicianRepo.findByUserId(userId)
                .map(TechnicianEntity::getId)
                .orElseThrow(() -> new NotFoundException("Technician profile not found for this user."));

        WorkOrderEntity wo = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Work order not found: " + id));

        if (!Objects.equals(wo.getAssignedTechId(), technicianId)) {
            throw new NotFoundException("TECH not allowed to complete this work order.");
        }

        if (wo.getStatus() != WorkOrderStatus.IN_PROGRESS &&
                wo.getStatus() != WorkOrderStatus.COMPLETED) {
            throw new RuntimeException("Only IN_PROGRESS or COMPLETED work orders can be signed off.");
        }

        if (req.signatureDataUrl() == null || req.signatureDataUrl().isBlank()) {
            throw new RuntimeException("Signature is required.");
        }

        String signatureUrl = saveSignatureFromDataUrl(req.signatureDataUrl(), wo.getId(), technicianId);

        wo.setSignatureUrl(signatureUrl);
        wo.setCompletionNotes(req.completionNotes());
        wo.setCompletedAt(Instant.now());
        wo.setStatus(WorkOrderStatus.COMPLETED);

        WorkOrderEntity saved = repo.save(wo);

        eventService.logCompleted(saved, req.completionNotes());

        return toDto(saved);
    }

    @Transactional
    public WorkOrderDto startWorkOrder(Long id, Long userId) {
        Long technicianId = technicianRepo.findByUserId(userId)
                .map(TechnicianEntity::getId)
                .orElseThrow(() -> new NotFoundException("Technician profile not found for this user."));

        WorkOrderEntity wo = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Work order not found: " + id));

        if (!Objects.equals(wo.getAssignedTechId(), technicianId)) {
            throw new NotFoundException("TECH not allowed to start this work order.");
        }

        if (wo.getStatus() == WorkOrderStatus.IN_PROGRESS) {
            return toDto(wo);
        }

        if (wo.getStatus() == WorkOrderStatus.COMPLETED) {
            throw new BusinessRuleException("Completed work orders cannot be started.");
        }

        if (wo.getStatus() == WorkOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled work orders cannot be started.");
        }

        if (wo.getStatus() != WorkOrderStatus.OPEN && wo.getStatus() != WorkOrderStatus.ASSIGNED) {
            throw new BusinessRuleException("Only OPEN or ASSIGNED work orders can be started.");
        }

        WorkOrderStatus previousStatus = wo.getStatus();
        wo.setStatus(WorkOrderStatus.IN_PROGRESS);

        WorkOrderEntity saved = repo.save(wo);
        eventService.logStarted(saved, previousStatus);

        return toDto(saved);
    }

    @Transactional
    public WorkOrderDto returnWorkOrderToOpen(Long id, Long userId) {
        TechnicianEntity technician = technicianRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Technician profile not found for this user."));

        WorkOrderEntity wo = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Work order not found: " + id));

        if (!Objects.equals(wo.getAssignedTechId(), technician.getId())) {
            throw new NotFoundException("TECH not allowed to release this work order.");
        }

        if (wo.getStatus() == WorkOrderStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Started work orders cannot be returned to OPEN from this screen.");
        }

        if (wo.getStatus() == WorkOrderStatus.COMPLETED) {
            throw new BusinessRuleException("Completed work orders cannot be returned to OPEN.");
        }

        if (wo.getStatus() == WorkOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled work orders are already closed.");
        }

        WorkOrderStatus previousStatus = wo.getStatus();
        Long previousTechId = wo.getAssignedTechId();

        wo.setAssignedTechId(null);
        wo.setStatus(WorkOrderStatus.OPEN);

        WorkOrderEntity saved = repo.save(wo);
        String actor = getCurrentActor();
        String technicianName = technician.getFullName() != null && !technician.getFullName().isBlank()
                ? technician.getFullName()
                : ("Tech#" + technician.getId());

        eventService.recordEvent(
                saved,
                WorkOrderEventType.UNASSIGNED_TECHNICIAN,
                "Technician " + technicianName + " released this work order so it can be reassigned.",
                previousTechId == null ? null : previousTechId.toString(),
                null,
                actor
        );

        if (previousStatus != WorkOrderStatus.OPEN) {
            eventService.recordEvent(
                    saved,
                    WorkOrderEventType.STATUS_CHANGED,
                    "Status changed to OPEN.",
                    previousStatus == null ? null : previousStatus.name(),
                    WorkOrderStatus.OPEN.name(),
                    actor
            );
        }

        return toDto(saved);
    }

    private String saveSignatureFromDataUrl(String dataUrl, Long workOrderId, Long technicianId) {
        try {
            if (!dataUrl.startsWith("data:image/")) {
                throw new RuntimeException("Invalid signature image format.");
            }

            String[] parts = dataUrl.split(",", 2);
            if (parts.length != 2) {
                throw new RuntimeException("Invalid signature data.");
            }

            String metadata = parts[0];
            String base64Data = parts[1];

            String extension = "png";
            if (metadata.contains("image/jpeg")) {
                extension = "jpg";
            } else if (metadata.contains("image/webp")) {
                extension = "webp";
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            String filename = "workorder_" + workOrderId + "_tech_" + technicianId + "_signature." + extension;

            return storageService.storeBytes(imageBytes, filename, "image/" + extension);

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Failed to decode signature image.", e);
        }
    }
    public ResponseEntity<?> getSignature(Long id) {
        WorkOrderEntity wo = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Work order not found: " + id));

        if (wo.getSignatureUrl() == null || wo.getSignatureUrl().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = storageService.loadAsResource(wo.getSignatureUrl());

        String contentType = "image/png";
        try {
            contentType = Files.probeContentType(Path.of(resource.getFile().getAbsolutePath()));
            if (contentType == null) {
                contentType = "image/png";
            }
        } catch (Exception ignored) {
        }

        String filename = Path.of(wo.getSignatureUrl()).getFileName().toString();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    @Transactional
    public WorkOrderCompletionResponse submitStructuredCompletionReport(
            Long workOrderId,
            WorkOrderCompletionRequest request,
            Long authenticatedUserId
    ) {
        Long technicianId = technicianRepo.findByUserId(authenticatedUserId)
                .map(TechnicianEntity::getId)
                .orElseThrow(() -> new NotFoundException("Technician profile not found for this user."));

        WorkOrderEntity workOrder = repo.findById(workOrderId)
                .orElseThrow(() -> new NotFoundException("Work order not found with id: " + workOrderId));

        if (workOrder.getAssignedTechId() == null) {
            throw new RuntimeException("Work order is not assigned to any technician");
        }

        if (!java.util.Objects.equals(workOrder.getAssignedTechId(), technicianId)) {
            throw new RuntimeException("You are not authorized to complete this work order");
        }

        if (workOrderCompletionRepository.existsByWorkOrderId(workOrderId)) {
            throw new BusinessRuleException("Work order has already been completed");
        }

        if (workOrder.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Only IN_PROGRESS work orders can be completed");
        }

        WorkOrderCompletionEntity completion = new WorkOrderCompletionEntity();
        completion.setWorkOrder(workOrder);
        completion.setFaTag(request.getFaTag());
        completion.setIssueResolved(request.getIssueResolved());
        completion.setReplacementNeeded(request.getReplacementNeeded());
        completion.setReturnVisitRequired(request.getReturnVisitRequired());
        completion.setSummaryOfWork(request.getSummaryOfWork());
        completion.setCompletedAt(LocalDateTime.now());
        completion.setCompletedByUserId(authenticatedUserId);

        WorkOrderCompletionEntity savedCompletion = workOrderCompletionRepository.save(completion);

        workOrder.setStatus(WorkOrderStatus.COMPLETED);
        workOrder.setCompletedAt(Instant.now());
        workOrder.setCompletionNotes(request.getSummaryOfWork());
        repo.save(workOrder);

        eventService.logCompleted(
                workOrder,
                "FA Tag: " + request.getFaTag()
                        + " | Issue Resolved: " + request.getIssueResolved()
                        + " | Replacement Needed: " + request.getReplacementNeeded()
                        + " | Return Visit Required: " + request.getReturnVisitRequired()
                        + " | Summary: " + request.getSummaryOfWork()
        );

        return mapToCompletionResponse(savedCompletion);
    }

    private WorkOrderCompletionResponse mapToCompletionResponse(WorkOrderCompletionEntity entity) {
        WorkOrderCompletionResponse response = new WorkOrderCompletionResponse();
        response.setId(entity.getId());
        response.setWorkOrderId(entity.getWorkOrder().getId());
        response.setFaTag(entity.getFaTag());
        response.setIssueResolved(entity.getIssueResolved());
        response.setReplacementNeeded(entity.getReplacementNeeded());
        response.setReturnVisitRequired(entity.getReturnVisitRequired());
        response.setSummaryOfWork(entity.getSummaryOfWork());
        response.setCompletedAt(entity.getCompletedAt());
        response.setCompletedByUserId(entity.getCompletedByUserId());
        return response;
    }

    @Transactional(readOnly = true)
    public WorkOrderCompletionResponse getCompletionByWorkOrderId(Long workOrderId) {
        WorkOrderCompletionEntity completion = workOrderCompletionRepository.findByWorkOrderId(workOrderId)
                .orElseThrow(() -> new NotFoundException("No completion report found for work order id: " + workOrderId));

        return mapToCompletionResponse(completion);
    }

    private String getCurrentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return "SYSTEM";
        }
        return auth.getName();
    }

    @Transactional
    public WorkOrderDto reopenWorkOrder(Long id, String reason) {
        WorkOrderEntity wo = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Work order not found: " + id));

        if (wo.getStatus() != WorkOrderStatus.COMPLETED) {
            throw new BusinessRuleException("Only COMPLETED work orders can be reopened.");
        }

        String previousSignatureUrl = wo.getSignatureUrl();

        wo.setStatus(WorkOrderStatus.OPEN);
        wo.setCompletedAt(null);
        wo.setCompletionNotes(null);
        wo.setSignatureUrl(null);
        wo.setAssignedTechId(null);

        workOrderCompletionRepository.findByWorkOrderId(id)
                .ifPresent(workOrderCompletionRepository::delete);

        WorkOrderEntity saved = repo.save(wo);
        String actor = getCurrentActor();

        if (previousSignatureUrl != null && !previousSignatureUrl.isBlank()) {
            try {
                storageService.delete(previousSignatureUrl);
            } catch (RuntimeException ex) {
                log.warn("Failed to delete reopened work order signature file {}", previousSignatureUrl, ex);
            }
        }

        eventService.logReopened(saved, reason);

        eventService.recordEvent(
                saved,
                WorkOrderEventType.STATUS_CHANGED,
                "Status changed to OPEN.",
                WorkOrderStatus.COMPLETED.name(),
                WorkOrderStatus.OPEN.name(),
                actor
        );

        return toDto(saved);
    }

}
