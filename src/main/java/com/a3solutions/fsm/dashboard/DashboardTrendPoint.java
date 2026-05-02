package com.a3solutions.fsm.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.dashboard
 * @project A3 Field Service Management Backend
 * @date 4/5/26
 */
@Schema(description = "Trend point used for dashboard line charts.")
public record DashboardTrendPoint(
       LocalDate date,
        String label,
        long total
) {
}
