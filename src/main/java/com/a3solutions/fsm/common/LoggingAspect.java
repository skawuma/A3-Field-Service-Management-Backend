package com.a3solutions.fsm.common;

import com.a3solutions.fsm.auth.UserDetailsImpl;
import com.a3solutions.fsm.security.Role;
import com.a3solutions.fsm.technician.TechnicianEntity;
import com.a3solutions.fsm.timesheet.TimesheetEntryResponse;
import com.a3solutions.fsm.timesheet.TimesheetResponse;
import com.a3solutions.fsm.timesheet.TimesheetStatus;
import com.a3solutions.fsm.workorder.WorkOrderEntity;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.common
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("execution(public * com.a3solutions.fsm.timesheet.TimesheetService.*(..))")
    public void timesheetServiceOperations() {
    }

    @Around("execution(* com.a3solutions.fsm..controller..*(..)) || " +
            "execution(* com.a3solutions.fsm..service..*(..)) || " +
            "execution(* com.a3solutions.fsm.technician.*Service.*(..)) || " +
            "execution(* com.a3solutions.fsm.workorder.*Service.*(..)) || " +
            "execution(* com.a3solutions.fsm.auth.AuthService.*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String method = joinPoint.getSignature().toShortString();
        log.info("Entering {}", method);

        try {
            Object result = joinPoint.proceed();
            long time = System.currentTimeMillis() - start;
            log.info("Exiting {} ({} ms)", method, time);
            return result;
        } catch (Throwable ex) {
            long time = System.currentTimeMillis() - start;
            log.error("Exception in {} after {} ms", method, time, ex);
            throw ex;
        }
    }

    @Around("timesheetServiceOperations()")
    public Object logTimesheetAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String operation = joinPoint.getSignature().getName();
        ActorContext actor = currentActor();
        TimesheetLogContext context = timesheetContext(operation, joinPoint.getArgs());

        log.info(
                "event=timesheet.operation.start operation={} actor={} actorUserId={} actorRole={} timesheetId={} technicianId={} entryId={} workOrderId={} status={} targetStatus={} weekStart={}",
                operation,
                actor.username(),
                actor.userId(),
                actor.role(),
                context.timesheetId(),
                context.technicianId(),
                context.entryId(),
                context.workOrderId(),
                context.status(),
                context.targetStatus(),
                context.weekStart()
        );

        try {
            Object result = joinPoint.proceed();
            long time = System.currentTimeMillis() - start;
            TimesheetLogContext resultContext = context.withResult(result);
            log.info(
                    "event=timesheet.operation.success operation={} actor={} actorUserId={} actorRole={} timesheetId={} technicianId={} entryId={} workOrderId={} status={} targetStatus={} weekStart={} resultCount={} responseBytes={} durationMs={}",
                    operation,
                    actor.username(),
                    actor.userId(),
                    actor.role(),
                    resultContext.timesheetId(),
                    resultContext.technicianId(),
                    resultContext.entryId(),
                    resultContext.workOrderId(),
                    resultContext.status(),
                    resultContext.targetStatus(),
                    resultContext.weekStart(),
                    resultContext.resultCount(),
                    resultContext.responseBytes(),
                    time
            );
            return result;
        } catch (Throwable ex) {
            long time = System.currentTimeMillis() - start;
            log.error(
                    "event=timesheet.operation.failure operation={} actor={} actorUserId={} actorRole={} timesheetId={} technicianId={} entryId={} workOrderId={} status={} targetStatus={} weekStart={} exceptionType={} durationMs={}",
                    operation,
                    actor.username(),
                    actor.userId(),
                    actor.role(),
                    context.timesheetId(),
                    context.technicianId(),
                    context.entryId(),
                    context.workOrderId(),
                    context.status(),
                    context.targetStatus(),
                    context.weekStart(),
                    ex.getClass().getSimpleName(),
                    time,
                    ex
            );
            throw ex;
        }
    }

    private TimesheetLogContext timesheetContext(String operation, Object[] args) {
        Long timesheetId = null;
        Long technicianId = null;
        Long entryId = null;
        Long workOrderId = null;
        TimesheetStatus status = null;
        TimesheetStatus targetStatus = targetStatus(operation);
        LocalDate weekStart = null;

        switch (operation) {
            case "getMyForWeek" -> weekStart = localDateArg(args, 1);
            case "getById", "submit", "approve", "reject", "exportCsv" -> timesheetId = longArg(args, 0);
            case "updateEntry" -> entryId = longArg(args, 0);
            case "getAll" -> {
                weekStart = localDateArg(args, 0);
                technicianId = longArg(args, 1);
                status = statusArg(args, 2);
            }
            case "autoAddCompletedWorkOrder" -> {
                WorkOrderEntity workOrder = arg(args, 0, WorkOrderEntity.class);
                TechnicianEntity technician = arg(args, 1, TechnicianEntity.class);
                workOrderId = workOrder == null ? null : workOrder.getId();
                technicianId = technician == null ? null : technician.getId();
            }
            default -> {
            }
        }

        return new TimesheetLogContext(
                timesheetId,
                technicianId,
                entryId,
                workOrderId,
                status,
                targetStatus,
                weekStart,
                null,
                null
        );
    }

    private ActorContext currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ActorContext(null, null, null);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl user) {
            return new ActorContext(user.getUsername(), user.getId(), user.getRole());
        }

        return new ActorContext(authentication.getName(), null, roleFromAuthorities(authentication));
    }

    private Role roleFromAuthorities(Authentication authentication) {
        if (authentication.getAuthorities() == null) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .map(this::safeRole)
                .filter(role -> role != null)
                .findFirst()
                .orElse(null);
    }

    private Role safeRole(String value) {
        try {
            return Role.valueOf(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private TimesheetStatus targetStatus(String operation) {
        return switch (operation) {
            case "submit" -> TimesheetStatus.SUBMITTED;
            case "approve" -> TimesheetStatus.APPROVED;
            case "reject" -> TimesheetStatus.REJECTED;
            default -> null;
        };
    }

    private Long longArg(Object[] args, int index) {
        Object value = arg(args, index, Object.class);
        return value instanceof Long longValue ? longValue : null;
    }

    private LocalDate localDateArg(Object[] args, int index) {
        Object value = arg(args, index, Object.class);
        return value instanceof LocalDate localDate ? localDate : null;
    }

    private TimesheetStatus statusArg(Object[] args, int index) {
        Object value = arg(args, index, Object.class);
        return value instanceof TimesheetStatus timesheetStatus ? timesheetStatus : null;
    }

    private <T> T arg(Object[] args, int index, Class<T> type) {
        if (args == null || index < 0 || index >= args.length || !type.isInstance(args[index])) {
            return null;
        }
        return type.cast(args[index]);
    }

    private record ActorContext(String username, Long userId, Role role) {
    }

    private record TimesheetLogContext(
            Long timesheetId,
            Long technicianId,
            Long entryId,
            Long workOrderId,
            TimesheetStatus status,
            TimesheetStatus targetStatus,
            LocalDate weekStart,
            Integer resultCount,
            Integer responseBytes
    ) {
        private TimesheetLogContext withResult(Object result) {
            if (result instanceof TimesheetResponse response) {
                return new TimesheetLogContext(
                        firstNonNull(timesheetId, response.id()),
                        firstNonNull(technicianId, response.technicianId()),
                        entryId,
                        workOrderId,
                        firstNonNull(response.status(), status),
                        targetStatus,
                        firstNonNull(weekStart, response.weekStartDate()),
                        response.entries() == null ? null : response.entries().size(),
                        responseBytes
                );
            }
            if (result instanceof TimesheetEntryResponse response) {
                return new TimesheetLogContext(
                        timesheetId,
                        technicianId,
                        firstNonNull(entryId, response.id()),
                        firstNonNull(workOrderId, response.workOrderId()),
                        status,
                        targetStatus,
                        weekStart,
                        resultCount,
                        responseBytes
                );
            }
            if (result instanceof List<?> responses) {
                return new TimesheetLogContext(
                        timesheetId,
                        technicianId,
                        entryId,
                        workOrderId,
                        status,
                        targetStatus,
                        weekStart,
                        responses.size(),
                        responseBytes
                );
            }
            if (result instanceof byte[] bytes) {
                return new TimesheetLogContext(
                        timesheetId,
                        technicianId,
                        entryId,
                        workOrderId,
                        status,
                        targetStatus,
                        weekStart,
                        resultCount,
                        bytes.length
                );
            }
            return this;
        }

        private static <T> T firstNonNull(T primary, T fallback) {
            return primary != null ? primary : fallback;
        }
    }
}
