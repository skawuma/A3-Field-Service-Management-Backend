package com.a3solutions.fsm.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.dashboard
 * @project A3 Field Service Management Backend
 * @date 4/5/26
 */
@Schema(description = "Single chart bucket with key, label, and total.")
public record DashboardChartDatum(
        String key,
        String label,
        long total
) {
}
