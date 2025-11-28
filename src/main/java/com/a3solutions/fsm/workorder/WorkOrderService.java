package com.a3solutions.fsm.workorder;

import com.a3solutions.fsm.common.PageResponse;
import com.a3solutions.fsm.exceptions.NotFoundException;
import com.a3solutions.fsm.security.Role;
import com.a3solutions.fsm.technician.TechnicianEntity;
import com.a3solutions.fsm.technician.TechnicianRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
@Service
public class WorkOrderService {

    private final WorkOrderRepository repo;
    private final TechnicianRepository technicianRepo;
    private final WorkOrderEventService eventService;

    public WorkOrderService(WorkOrderRepository repo, TechnicianRepository technicianRepo, WorkOrderEventService eventService) {
        this.repo = repo;
        this.technicianRepo = technicianRepo;
        this.eventService = eventService;
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
        wo.setStatus(WorkOrderStatus.IN_PROGRESS);

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
        if (oldStatus != WorkOrderStatus.IN_PROGRESS) {
            eventService.recordEvent(
                    saved,
                    WorkOrderEventType.STATUS_CHANGED,
                    "Status changed to IN_PROGRESS.",
                    oldStatus.name(),
                    WorkOrderStatus.IN_PROGRESS.name(),
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
    public boolean canTechAccessWorkOrder(Long workOrderId, Long userId) {
        var techOpt = technicianRepo.findByUserId(userId);
        if (techOpt.isEmpty()) {
            return false;
        }
        Long technicianId = techOpt.get().getId();

        return repo.findById(workOrderId)
                .map(wo -> technicianId.equals(wo.getAssignedTechId()))
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
                e.getPriority()
        );
    }
}
