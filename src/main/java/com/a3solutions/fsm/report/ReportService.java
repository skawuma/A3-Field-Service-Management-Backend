package com.a3solutions.fsm.report;

import com.a3solutions.fsm.exceptions.BadRequestException;
import com.a3solutions.fsm.technician.TechnicianEntity;
import com.a3solutions.fsm.technician.TechnicianRepository;
import com.a3solutions.fsm.workorder.WorkOrderEntity;
import com.a3solutions.fsm.workorder.WorkOrderRepository;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class ReportService {
    private static final int MAX_REPORT_DAYS = 366;
    private static final List<WorkOrderStatus> TERMINAL_STATUSES = List.of(
            WorkOrderStatus.COMPLETED,
            WorkOrderStatus.CANCELLED
    );

    private final WorkOrderRepository workOrderRepository;
    private final TechnicianRepository technicianRepository;

    public ReportService(
            WorkOrderRepository workOrderRepository,
            TechnicianRepository technicianRepository
    ) {
        this.workOrderRepository = workOrderRepository;
        this.technicianRepository = technicianRepository;
    }

    @Transactional(readOnly = true)
    public OperationsReport getOperationsReport(LocalDate from, LocalDate to) {
        validatePeriod(from, to);

        ZoneId zone = ZoneId.systemDefault();
        Map<Long, String> technicianNames = technicianRepository.findAll().stream()
                .collect(LinkedHashMap::new, (names, technician) ->
                        names.put(technician.getId(), technician.getFullName()), Map::putAll);

        List<WorkOrderEntity> workOrders = workOrderRepository.findAll().stream()
                .filter(workOrder -> isWithinPeriod(workOrder, from, to, zone))
                .sorted(Comparator.comparing(WorkOrderEntity::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        return new OperationsReport(
                Instant.now(),
                from,
                to,
                "Organization-wide",
                buildSummary(workOrders),
                buildStatusBuckets(workOrders),
                buildPriorityBuckets(workOrders),
                buildTechnicianPerformance(workOrders, technicianNames),
                buildWorkOrderRows(workOrders, technicianNames)
        );
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BadRequestException("Report start and end dates are required.");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("Report start date must be on or before the end date.");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_REPORT_DAYS) {
            throw new BadRequestException("Report period cannot exceed 366 days.");
        }
    }

    private boolean isWithinPeriod(WorkOrderEntity workOrder, LocalDate from, LocalDate to, ZoneId zone) {
        if (workOrder.getCreatedAt() == null) {
            return false;
        }
        LocalDate createdDate = workOrder.getCreatedAt().atZone(zone).toLocalDate();
        return !createdDate.isBefore(from) && !createdDate.isAfter(to);
    }

    private OperationsReportSummary buildSummary(List<WorkOrderEntity> workOrders) {
        long active = workOrders.stream().filter(workOrder -> !TERMINAL_STATUSES.contains(workOrder.getStatus())).count();
        long completed = countStatus(workOrders, WorkOrderStatus.COMPLETED);
        long cancelled = countStatus(workOrders, WorkOrderStatus.CANCELLED);
        long breached = workOrders.stream().filter(workOrder -> Boolean.TRUE.equals(workOrder.getSlaBreached())).count();
        List<WorkOrderEntity> completedWithSla = workOrders.stream()
                .filter(workOrder -> workOrder.getStatus() == WorkOrderStatus.COMPLETED)
                .filter(workOrder -> workOrder.getSlaClockStartedAt() != null)
                .toList();
        long completedWithinSla = completedWithSla.stream()
                .filter(workOrder -> !Boolean.TRUE.equals(workOrder.getSlaBreached()))
                .count();

        return new OperationsReportSummary(
                workOrders.size(),
                active,
                completed,
                cancelled,
                breached,
                completedWithinSla,
                percent(completedWithinSla, completedWithSla.size()),
                averageDuration(workOrders, DurationMetric.ASSIGN),
                averageDuration(workOrders, DurationMetric.START),
                averageDuration(workOrders, DurationMetric.RESOLUTION)
        );
    }

    private List<OperationsReportBucket> buildStatusBuckets(List<WorkOrderEntity> workOrders) {
        return Arrays.stream(WorkOrderStatus.values())
                .map(status -> new OperationsReportBucket(
                        status.name(),
                        label(status.name()),
                        countStatus(workOrders, status)
                ))
                .filter(bucket -> bucket.total() > 0)
                .toList();
    }

    private List<OperationsReportBucket> buildPriorityBuckets(List<WorkOrderEntity> workOrders) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String priority : List.of("CRITICAL", "HIGH", "MEDIUM", "LOW", "UNSPECIFIED")) {
            counts.put(priority, 0L);
        }
        workOrders.forEach(workOrder -> counts.compute(
                normalizePriority(workOrder.getPriority()),
                (priority, total) -> total == null ? 1L : total + 1L
        ));
        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> new OperationsReportBucket(entry.getKey(), label(entry.getKey()), entry.getValue()))
                .toList();
    }

    private List<OperationsReportTechnician> buildTechnicianPerformance(
            List<WorkOrderEntity> workOrders,
            Map<Long, String> technicianNames
    ) {
        Map<Long, List<WorkOrderEntity>> completedByTechnician = new LinkedHashMap<>();
        workOrders.stream()
                .filter(workOrder -> workOrder.getStatus() == WorkOrderStatus.COMPLETED)
                .filter(workOrder -> workOrder.getAssignedTechId() != null)
                .forEach(workOrder -> completedByTechnician
                        .computeIfAbsent(workOrder.getAssignedTechId(), ignored -> new ArrayList<>())
                        .add(workOrder));

        return completedByTechnician.entrySet().stream()
                .map(entry -> {
                    List<WorkOrderEntity> completed = entry.getValue();
                    long breached = completed.stream()
                            .filter(workOrder -> Boolean.TRUE.equals(workOrder.getSlaBreached()))
                            .count();
                    long within = completed.size() - breached;
                    return new OperationsReportTechnician(
                            entry.getKey(),
                            technicianNames.getOrDefault(entry.getKey(), "Technician #" + entry.getKey()),
                            completed.size(),
                            within,
                            breached,
                            percent(within, completed.size()),
                            averageCompletionDuration(completed)
                    );
                })
                .sorted(Comparator.comparingDouble(OperationsReportTechnician::compliancePercent).reversed()
                        .thenComparing(OperationsReportTechnician::technicianName))
                .toList();
    }

    private List<OperationsReportWorkOrder> buildWorkOrderRows(
            List<WorkOrderEntity> workOrders,
            Map<Long, String> technicianNames
    ) {
        return workOrders.stream()
                .map(workOrder -> new OperationsReportWorkOrder(
                        workOrder.getId(),
                        workOrder.getId() == null ? "WO" : "WO-" + workOrder.getId(),
                        workOrder.getClientName(),
                        workOrder.getDescription(),
                        workOrder.getStatus() == null ? "UNKNOWN" : workOrder.getStatus().name(),
                        normalizePriority(workOrder.getPriority()),
                        workOrder.getScheduledDate(),
                        workOrder.getAssignedTechId() == null
                                ? "Unassigned"
                                : technicianNames.getOrDefault(workOrder.getAssignedTechId(),
                                        "Technician #" + workOrder.getAssignedTechId()),
                        slaOutcome(workOrder),
                        durationMinutes(workOrder.getCreatedAt(), workOrder.getCompletedAt()),
                        workOrder.getCreatedAt(),
                        workOrder.getCompletedAt()
                ))
                .toList();
    }

    private long countStatus(List<WorkOrderEntity> workOrders, WorkOrderStatus status) {
        return workOrders.stream().filter(workOrder -> workOrder.getStatus() == status).count();
    }

    private String slaOutcome(WorkOrderEntity workOrder) {
        if (workOrder.getStatus() != WorkOrderStatus.COMPLETED) {
            return Boolean.TRUE.equals(workOrder.getSlaBreached()) ? "BREACHED" : "PENDING";
        }
        if (workOrder.getSlaClockStartedAt() == null) {
            return "NOT_TRACKED";
        }
        return Boolean.TRUE.equals(workOrder.getSlaBreached()) ? "BREACHED" : "MET";
    }

    private Long averageDuration(List<WorkOrderEntity> workOrders, DurationMetric metric) {
        List<Long> durations = workOrders.stream()
                .map(workOrder -> switch (metric) {
                    case ASSIGN -> durationMinutes(workOrder.getCreatedAt(), workOrder.getAssignedAt());
                    case START -> durationMinutes(workOrder.getAssignedAt(), workOrder.getSlaClockStartedAt());
                    case RESOLUTION -> durationMinutes(workOrder.getCreatedAt(), workOrder.getCompletedAt());
                })
                .filter(Objects::nonNull)
                .toList();
        return roundedAverage(durations);
    }

    private Long averageCompletionDuration(List<WorkOrderEntity> workOrders) {
        List<Long> durations = workOrders.stream()
                .map(workOrder -> workOrder.getActualCompletionMinutes() == null
                        ? durationMinutes(workOrder.getSlaClockStartedAt(), workOrder.getCompletedAt())
                        : workOrder.getActualCompletionMinutes().longValue())
                .filter(Objects::nonNull)
                .toList();
        return roundedAverage(durations);
    }

    private Long roundedAverage(List<Long> durations) {
        if (durations.isEmpty()) {
            return null;
        }
        return Math.round(durations.stream().mapToLong(Long::longValue).average().orElse(0));
    }

    private Long durationMinutes(Instant start, Instant end) {
        if (start == null || end == null || end.isBefore(start)) {
            return null;
        }
        return Duration.between(start, end).toMinutes();
    }

    private double percent(long numerator, long denominator) {
        if (denominator == 0) {
            return 0;
        }
        return Math.round((numerator * 1000.0) / denominator) / 10.0;
    }

    private String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return "UNSPECIFIED";
        }
        return priority.trim().toUpperCase(Locale.ROOT);
    }

    private String label(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Arrays.stream(normalized.split(" "))
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .reduce((left, right) -> left + " " + right)
                .orElse(value);
    }

    private enum DurationMetric {
        ASSIGN,
        START,
        RESOLUTION
    }
}
