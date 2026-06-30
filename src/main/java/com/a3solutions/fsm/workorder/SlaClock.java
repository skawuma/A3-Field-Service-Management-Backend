package com.a3solutions.fsm.workorder;

import java.time.Duration;
import java.time.Instant;

final class SlaClock {
    private SlaClock() {
    }

    static int durationForPriority(String priority) {
        if (priority == null) {
            return 240;
        }
        return switch (priority.trim().toUpperCase()) {
            case "CRITICAL" -> 60;
            case "HIGH" -> 120;
            case "LOW" -> 480;
            default -> 240;
        };
    }

    static long minutesBetween(Instant start, Instant end) {
        if (start == null || end == null) {
            return 0;
        }
        return Math.max(0, Duration.between(start, end).toMinutes());
    }

    static Long nullableMinutesBetween(Instant start, Instant end) {
        return start == null || end == null ? null : minutesBetween(start, end);
    }
}
