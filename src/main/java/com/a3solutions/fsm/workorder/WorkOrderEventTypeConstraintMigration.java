package com.a3solutions.fsm.workorder;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Keeps older local databases compatible when the work order event lifecycle expands.
 * Hibernate's ddl-auto=update does not rewrite existing CHECK constraints.
 */
@Component
public class WorkOrderEventTypeConstraintMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public WorkOrderEventTypeConstraintMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String allowedEventTypes = Arrays.stream(WorkOrderEventType.values())
                .map(WorkOrderEventType::name)
                .map(name -> "'" + name + "'")
                .collect(Collectors.joining(", "));

        jdbcTemplate.execute("ALTER TABLE work_order_events DROP CONSTRAINT IF EXISTS work_order_events_event_type_check");
        jdbcTemplate.execute("""
                ALTER TABLE work_order_events
                ADD CONSTRAINT work_order_events_event_type_check
                CHECK (
                    event_type IN (%s)
                )
                """.formatted(allowedEventTypes));
    }
}
