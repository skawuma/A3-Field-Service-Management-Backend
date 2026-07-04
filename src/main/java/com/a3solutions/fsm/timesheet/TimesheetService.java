package com.a3solutions.fsm.timesheet;

import com.a3solutions.fsm.exceptions.BusinessRuleException;
import com.a3solutions.fsm.exceptions.NotFoundException;
import com.a3solutions.fsm.security.Role;
import com.a3solutions.fsm.technician.TechnicianEntity;
import com.a3solutions.fsm.technician.TechnicianRepository;
import com.a3solutions.fsm.workorder.WorkOrderEntity;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import com.a3solutions.fsm.workordercompletion.WorkOrderCompletionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.timesheet
 * @project A3 Field Service Management Backend
 * @date 07/03/26
 */
@Service
public class TimesheetService {

    private final TimesheetRepository timesheetRepository;
    private final TimesheetEntryRepository entryRepository;
    private final TechnicianRepository technicianRepository;
    private final WorkOrderCompletionRepository completionRepository;
    private final ZoneId payrollZone;

    public TimesheetService(
            TimesheetRepository timesheetRepository,
            TimesheetEntryRepository entryRepository,
            TechnicianRepository technicianRepository,
            WorkOrderCompletionRepository completionRepository,
            @Value("${app.timesheets.zone-id:America/New_York}") String payrollZoneId
    ) {
        this.timesheetRepository = timesheetRepository;
        this.entryRepository = entryRepository;
        this.technicianRepository = technicianRepository;
        this.completionRepository = completionRepository;
        this.payrollZone = ZoneId.of(payrollZoneId);
    }

    @Transactional
    public TimesheetResponse getMyCurrent(Long userId) {
        TechnicianEntity technician = getTechnicianForUser(userId);
        return toResponse(findOrCreateWeeklyTimesheet(technician, LocalDate.now(payrollZone)));
    }

    @Transactional
    public TimesheetResponse getMyForWeek(Long userId, LocalDate weekStart) {
        TechnicianEntity technician = getTechnicianForUser(userId);
        LocalDate normalizedWeekStart = normalizeWeekStart(weekStart);
        return toResponse(findOrCreateWeeklyTimesheet(technician, normalizedWeekStart));
    }

    @Transactional(readOnly = true)
    public TimesheetResponse getById(Long id, Long userId, Role role) {
        TimesheetEntity timesheet = getTimesheet(id);
        assertCanView(timesheet, userId, role);
        return toResponse(timesheet);
    }

    @Transactional(readOnly = true)
    public List<TimesheetResponse> getAll(
            LocalDate weekStart,
            Long technicianId,
            TimesheetStatus status
    ) {
        Specification<TimesheetEntity> specification = (root, query, builder) -> builder.conjunction();
        if (weekStart != null) {
            LocalDate normalized = normalizeWeekStart(weekStart);
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("weekStartDate"), normalized));
        }
        if (technicianId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("technicianId"), technicianId));
        }
        if (status != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
        }

        Sort sort = Sort.by(
                Sort.Order.desc("weekStartDate"),
                Sort.Order.asc("technicianName")
        );
        return timesheetRepository.findAll(specification, sort).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TimesheetEntryResponse updateEntry(
            Long entryId,
            TimesheetEntryUpdateRequest request,
            Long userId
    ) {
        TimesheetEntryEntity entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new NotFoundException("Timesheet entry not found: " + entryId));
        TimesheetEntity timesheet = getTimesheet(entry.getTimesheetId());
        TechnicianEntity technician = getTechnicianForUser(userId);

        if (!Objects.equals(timesheet.getTechnicianId(), technician.getId())) {
            throw new AccessDeniedException("TECH can only edit their own timesheet entries.");
        }
        if (timesheet.getStatus() != TimesheetStatus.DRAFT
                && timesheet.getStatus() != TimesheetStatus.REJECTED) {
            throw new BusinessRuleException("Only draft or rejected timesheets can be edited.");
        }
        if (request.miles() != null && request.miles().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Miles cannot be negative.");
        }

        validateTimeRange(
                request.onsiteStartTime(),
                request.breakStartTime(),
                request.breakEndTime(),
                request.offsiteEndTime()
        );

        entry.setMiles(request.miles());
        entry.setOnsiteStartTime(request.onsiteStartTime());
        entry.setBreakStartTime(request.breakStartTime());
        entry.setBreakEndTime(request.breakEndTime());
        entry.setOffsiteEndTime(request.offsiteEndTime());
        entry.setComments(trimToNull(request.comments()));

        if (timesheet.getStatus() == TimesheetStatus.REJECTED) {
            timesheet.setStatus(TimesheetStatus.DRAFT);
            timesheet.setSubmittedAt(null);
            timesheet.setApprovedAt(null);
            timesheetRepository.save(timesheet);
        }

        return toEntryResponse(entryRepository.save(entry));
    }

    @Transactional
    public TimesheetResponse submit(Long id, TimesheetSubmitRequest request, Long userId) {
        TimesheetEntity timesheet = getTimesheet(id);
        TechnicianEntity technician = getTechnicianForUser(userId);
        if (!Objects.equals(timesheet.getTechnicianId(), technician.getId())) {
            throw new AccessDeniedException("TECH can only submit their own timesheet.");
        }
        if (timesheet.getStatus() != TimesheetStatus.DRAFT
                && timesheet.getStatus() != TimesheetStatus.REJECTED) {
            throw new BusinessRuleException("Only draft or rejected timesheets can be submitted.");
        }
        if (request == null || request.technicianSignatureText() == null
                || request.technicianSignatureText().isBlank()) {
            throw new BusinessRuleException("Technician signature is required.");
        }

        List<TimesheetEntryEntity> entries = entriesFor(timesheet.getId());
        if (entries.isEmpty()) {
            throw new BusinessRuleException("A timesheet must contain at least one work entry before submission.");
        }
        entries.forEach(this::validateEntryForSubmission);

        timesheet.setTechnicianSignatureText(request.technicianSignatureText().trim());
        timesheet.setStatus(TimesheetStatus.SUBMITTED);
        timesheet.setSubmittedAt(Instant.now());
        timesheet.setApprovedAt(null);
        return toResponse(timesheetRepository.save(timesheet));
    }

    @Transactional
    public TimesheetResponse approve(Long id, Role role) {
        assertPayrollReviewer(role);
        TimesheetEntity timesheet = getTimesheet(id);
        if (timesheet.getStatus() != TimesheetStatus.SUBMITTED) {
            throw new BusinessRuleException("Only submitted timesheets can be approved.");
        }
        timesheet.setStatus(TimesheetStatus.APPROVED);
        timesheet.setApprovedAt(Instant.now());
        return toResponse(timesheetRepository.save(timesheet));
    }

    @Transactional
    public TimesheetResponse reject(Long id, Role role) {
        assertPayrollReviewer(role);
        TimesheetEntity timesheet = getTimesheet(id);
        if (timesheet.getStatus() != TimesheetStatus.SUBMITTED) {
            throw new BusinessRuleException("Only submitted timesheets can be rejected.");
        }
        timesheet.setStatus(TimesheetStatus.REJECTED);
        timesheet.setApprovedAt(null);
        return toResponse(timesheetRepository.save(timesheet));
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(Long id, Long userId, Role role) {
        TimesheetEntity timesheet = getTimesheet(id);
        assertCanView(timesheet, userId, role);
        List<TimesheetEntryEntity> entries = entriesFor(timesheet.getId());

        String locations = entries.stream()
                .map(this::formatLocation)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.joining(" | "));

        List<String> rows = new ArrayList<>();
        rows.add(csvRow("Tech Name", timesheet.getTechnicianName()));
        rows.add(csvRow("Week", timesheet.getWeekStartDate() + " through " + timesheet.getWeekEndDate()));
        rows.add(csvRow("Project Site Location", locations));
        rows.add(csvRow("Status", timesheet.getStatus().name()));
        rows.add("");
        rows.add(csvRow(
                "Date",
                "Miles",
                "Start Time Onsite",
                "Break Start",
                "Break End",
                "End Time Offsite",
                "Comments"
        ));
        entries.forEach(entry -> rows.add(csvRow(
                entry.getWorkDate(),
                entry.getMiles(),
                entry.getOnsiteStartTime(),
                entry.getBreakStartTime(),
                entry.getBreakEndTime(),
                entry.getOffsiteEndTime(),
                entry.getComments()
        )));
        rows.add("");
        rows.add(csvRow("Technician Signature", timesheet.getTechnicianSignatureText()));
        rows.add(csvRow("Generated", Instant.now().toString()));

        return ("\uFEFF" + String.join("\r\n", rows)).getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public void autoAddCompletedWorkOrder(WorkOrderEntity workOrder, TechnicianEntity technician) {
        if (workOrder == null || workOrder.getId() == null
                || workOrder.getStatus() != WorkOrderStatus.COMPLETED) {
            throw new BusinessRuleException("Only persisted completed work orders can create timesheet entries.");
        }
        if (technician == null || technician.getId() == null
                || !Objects.equals(workOrder.getAssignedTechId(), technician.getId())) {
            throw new BusinessRuleException("Completed work order technician is missing or inconsistent.");
        }
        if (entryRepository.existsByWorkOrderId(workOrder.getId())) {
            return;
        }

        Instant completedAt = Optional.ofNullable(workOrder.getCompletedAt()).orElseGet(Instant::now);
        LocalDate workDate = completedAt.atZone(payrollZone).toLocalDate();
        TimesheetEntity timesheet = findOrCreateWeeklyTimesheet(technician, workDate);
        SiteParts site = parseSite(workOrder.getAddress());
        String completionSummary = completionRepository.findByWorkOrderId(workOrder.getId())
                .map(completion -> completion.getSummaryOfWork())
                .orElse(null);

        TimesheetEntryEntity entry = TimesheetEntryEntity.builder()
                .timesheetId(timesheet.getId())
                .workOrderId(workOrder.getId())
                .workDate(workDate)
                .clientName(fallback(workOrder.getClientName(), "Client not specified"))
                .siteAddress(site.street())
                .city(site.city())
                .state(site.state())
                .zip(site.zip())
                .onsiteStartTime(toLocalTime(firstNonNull(
                        workOrder.getArrivedAt(),
                        workOrder.getWorkStartedAt()
                )))
                .offsiteEndTime(toLocalTime(completedAt))
                .comments(combineComments(completionSummary, workOrder.getCompletionNotes()))
                .autoGenerated(true)
                .build();
        entryRepository.save(entry);
    }

    LocalDate normalizeWeekStart(LocalDate date) {
        LocalDate effective = date == null ? LocalDate.now(payrollZone) : date;
        return effective.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private TimesheetEntity findOrCreateWeeklyTimesheet(TechnicianEntity technician, LocalDate dateInWeek) {
        LocalDate weekStart = normalizeWeekStart(dateInWeek);
        return timesheetRepository.findByTechnicianIdAndWeekStartDate(technician.getId(), weekStart)
                .orElseGet(() -> timesheetRepository.save(TimesheetEntity.builder()
                        .technicianId(technician.getId())
                        .technicianName(fallback(technician.getFullName(), "Technician #" + technician.getId()))
                        .weekStartDate(weekStart)
                        .weekEndDate(weekStart.plusDays(6))
                        .status(TimesheetStatus.DRAFT)
                        .build()));
    }

    private void validateEntryForSubmission(TimesheetEntryEntity entry) {
        if (entry.getWorkDate() == null || entry.getClientName() == null || entry.getClientName().isBlank()) {
            throw new BusinessRuleException("Every timesheet entry requires a work date and client name.");
        }
        if (entry.getMiles() != null && entry.getMiles().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Miles cannot be negative.");
        }
        validateTimeRange(
                entry.getOnsiteStartTime(),
                entry.getBreakStartTime(),
                entry.getBreakEndTime(),
                entry.getOffsiteEndTime()
        );
    }

    private void validateTimeRange(
            LocalTime onsiteStart,
            LocalTime breakStart,
            LocalTime breakEnd,
            LocalTime offsiteEnd
    ) {
        if ((breakStart == null) != (breakEnd == null)) {
            throw new BusinessRuleException("Break start and break end must be entered together.");
        }
        if (onsiteStart != null && offsiteEnd != null && offsiteEnd.isBefore(onsiteStart)) {
            throw new BusinessRuleException("Offsite end time cannot be before onsite start time.");
        }
        if (breakStart != null && breakEnd.isBefore(breakStart)) {
            throw new BusinessRuleException("Break end time cannot be before break start time.");
        }
        if (onsiteStart != null && breakStart != null && breakStart.isBefore(onsiteStart)) {
            throw new BusinessRuleException("Break start time cannot be before onsite start time.");
        }
        if (offsiteEnd != null && breakEnd != null && breakEnd.isAfter(offsiteEnd)) {
            throw new BusinessRuleException("Break end time cannot be after offsite end time.");
        }
    }

    private void assertCanView(TimesheetEntity timesheet, Long userId, Role role) {
        if (role == Role.ADMIN || role == Role.DISPATCH) {
            return;
        }
        if (role != Role.TECH) {
            throw new AccessDeniedException("Timesheet access denied.");
        }
        TechnicianEntity technician = getTechnicianForUser(userId);
        if (!Objects.equals(timesheet.getTechnicianId(), technician.getId())) {
            throw new AccessDeniedException("TECH can only access their own timesheets.");
        }
    }

    private void assertPayrollReviewer(Role role) {
        if (role != Role.ADMIN && role != Role.DISPATCH) {
            throw new AccessDeniedException("Only ADMIN or DISPATCH can review timesheets.");
        }
    }

    private TechnicianEntity getTechnicianForUser(Long userId) {
        return technicianRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Technician profile not found for this user."));
    }

    private TimesheetEntity getTimesheet(Long id) {
        return timesheetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Timesheet not found: " + id));
    }

    private List<TimesheetEntryEntity> entriesFor(Long timesheetId) {
        return entryRepository.findByTimesheetIdOrderByWorkDateAscIdAsc(timesheetId);
    }

    private TimesheetResponse toResponse(TimesheetEntity timesheet) {
        return new TimesheetResponse(
                timesheet.getId(),
                timesheet.getTechnicianId(),
                timesheet.getTechnicianName(),
                timesheet.getWeekStartDate(),
                timesheet.getWeekEndDate(),
                timesheet.getStatus(),
                timesheet.getSubmittedAt(),
                timesheet.getApprovedAt(),
                timesheet.getTechnicianSignatureText(),
                timesheet.getCreatedAt(),
                timesheet.getUpdatedAt(),
                entriesFor(timesheet.getId()).stream().map(this::toEntryResponse).toList()
        );
    }

    private TimesheetEntryResponse toEntryResponse(TimesheetEntryEntity entry) {
        return new TimesheetEntryResponse(
                entry.getId(),
                entry.getWorkOrderId(),
                entry.getWorkDate(),
                entry.getClientName(),
                entry.getSiteAddress(),
                entry.getCity(),
                entry.getState(),
                entry.getZip(),
                entry.getMiles(),
                entry.getOnsiteStartTime(),
                entry.getBreakStartTime(),
                entry.getBreakEndTime(),
                entry.getOffsiteEndTime(),
                entry.getComments(),
                Boolean.TRUE.equals(entry.getAutoGenerated()),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }

    private SiteParts parseSite(String address) {
        if (address == null || address.isBlank()) {
            return new SiteParts(null, null, null, null);
        }
        List<String> parts = Arrays.stream(address.split(","))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
        if (parts.size() < 3) {
            return new SiteParts(address.trim(), null, null, null);
        }

        String street = String.join(", ", parts.subList(0, parts.size() - 2));
        String city = parts.get(parts.size() - 2);
        String stateAndZip = parts.get(parts.size() - 1);
        String[] stateParts = stateAndZip.split("\\s+", 2);
        String state = stateParts[0].toUpperCase(Locale.ROOT);
        String zip = stateParts.length > 1 ? trimToNull(stateParts[1]) : null;
        return new SiteParts(street, city, state, zip);
    }

    private String formatLocation(TimesheetEntryEntity entry) {
        List<String> parts = new ArrayList<>();
        if (entry.getClientName() != null) parts.add(entry.getClientName());
        if (entry.getSiteAddress() != null) parts.add(entry.getSiteAddress());
        if (entry.getCity() != null) parts.add(entry.getCity());
        String stateZip = Arrays.asList(entry.getState(), entry.getZip()).stream()
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" "));
        if (!stateZip.isBlank()) parts.add(stateZip);
        return String.join(", ", parts);
    }

    private String csvRow(Object... values) {
        return Arrays.stream(values)
                .map(value -> value == null ? "" : String.valueOf(value))
                .map(value -> "\"" + value.replace("\"", "\"\"") + "\"")
                .collect(Collectors.joining(","));
    }

    private String combineComments(String first, String second) {
        String normalizedFirst = trimToNull(first);
        String normalizedSecond = trimToNull(second);
        if (normalizedFirst == null) return normalizedSecond;
        if (normalizedSecond == null || normalizedFirst.equals(normalizedSecond)) return normalizedFirst;
        return normalizedFirst + " — " + normalizedSecond;
    }

    private String fallback(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Instant firstNonNull(Instant first, Instant second) {
        return first != null ? first : second;
    }

    private LocalTime toLocalTime(Instant value) {
        return value == null ? null : value.atZone(payrollZone).toLocalTime().withNano(0);
    }

    private record SiteParts(String street, String city, String state, String zip) {
    }
}
