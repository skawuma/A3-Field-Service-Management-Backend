package com.a3solutions.fsm.workorder;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Keeps older local databases compatible when the work order status lifecycle expands.
 * Hibernate's ddl-auto=update does not rewrite existing CHECK constraints.
 */
@Component
public class WorkOrderStatusConstraintMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public WorkOrderStatusConstraintMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String allowedStatuses = Arrays.stream(WorkOrderStatus.values())
                .map(WorkOrderStatus::name)
                .map(name -> "'" + name + "'")
                .collect(Collectors.joining(", "));
        jdbcTemplate.execute("ALTER TABLE work_orders DROP CONSTRAINT IF EXISTS work_orders_status_check");
        jdbcTemplate.execute("""
                ALTER TABLE work_orders
                ADD CONSTRAINT work_orders_status_check
                CHECK (
                    status IN (%s)
                )
                """.formatted(allowedStatuses));
    }
}
