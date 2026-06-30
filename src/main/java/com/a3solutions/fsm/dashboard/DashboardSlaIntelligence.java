package com.a3solutions.fsm.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Sprint 11 multi-clock SLA operational intelligence.")
public record DashboardSlaIntelligence(
        long nearBreachCount,
        long breachedActiveCount,
        long completedWithinSlaCount,
        long completedBreachedCount,
        Long averageTimeToAssignMinutes,
        Long averageTimeToStartMinutes,
        Long averageTimeToCompleteMinutes,
        List<DashboardTechnicianSlaPerformance> technicianPerformance
) {
}
