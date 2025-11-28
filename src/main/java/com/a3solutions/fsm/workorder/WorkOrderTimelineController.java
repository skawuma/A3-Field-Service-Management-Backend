package com.a3solutions.fsm.workorder;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3_SOLUTIONS_PROJECT
 * @date 11/26/25
 */
@RestController
@RequestMapping("/api/workorders")
public class WorkOrderTimelineController {

    private final WorkOrderEventService eventService;

    public WorkOrderTimelineController(WorkOrderEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/{id}/events")
    public List<WorkOrderEventDto> getTimeline(@PathVariable("id") Long workOrderId) {
        return eventService.getTimeline(workOrderId)
                .stream()
                .map(eventService::toDto)
                .toList();
    }
}
