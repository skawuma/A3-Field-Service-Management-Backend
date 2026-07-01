package com.a3solutions.fsm.report;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Chart-ready operations report bucket.")
public record OperationsReportBucket(
        String key,
        String label,
        long total
) {
}
