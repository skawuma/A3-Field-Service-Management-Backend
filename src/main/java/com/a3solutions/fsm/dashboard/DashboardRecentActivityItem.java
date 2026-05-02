package com.a3solutions.fsm.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.dashboard
 * @project A3 Field Service Management Backend
 * @date 4/5/26
 */
@Schema(description = "Dashboard-friendly recent activity item.")
public record DashboardRecentActivityItem(
        Long workOrderId,
        String eventType,
        String title,
        String description,
        String actor,
        Instant createdAt
) {
}
