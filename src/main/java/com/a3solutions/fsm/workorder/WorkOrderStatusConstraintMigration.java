package com.a3solutions.fsm.workorder;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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
        jdbcTemplate.execute("ALTER TABLE work_orders DROP CONSTRAINT IF EXISTS work_orders_status_check");
        jdbcTemplate.execute("""
                ALTER TABLE work_orders
                ADD CONSTRAINT work_orders_status_check
                CHECK (
                    status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
                )
                """);
    }
}
