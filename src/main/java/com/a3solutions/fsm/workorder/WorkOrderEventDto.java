package com.a3solutions.fsm.workorder;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3_SOLUTIONS_PROJECT
 * @date 11/26/25
 */
@Schema(description = "Timeline event recorded against a work order.")
public class WorkOrderEventDto {


    @Schema(description = "Event identifier.", example = "84")
    private Long id;
    @Schema(description = "Event type.", example = "ASSIGNED_TECHNICIAN")
    private WorkOrderEventType eventType;
    @Schema(description = "Timestamp when the event was recorded.", example = "2026-05-02T14:30:00Z")
    private Instant createdAt;
    @Schema(description = "Human-readable event message.", example = "Work order assigned to Deborah Katimbo.")
    private String message;
    @Schema(description = "Previous value when applicable.", example = "OPEN")
    private String oldValue;
    @Schema(description = "New value when applicable.", example = "ASSIGNED")
    private String newValue;
    @Schema(description = "Authenticated actor responsible for the change.", example = "admin@a3fsm.com")
    private String actor;

    // getters & setters...

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WorkOrderEventType getEventType() { return eventType; }
    public void setEventType(WorkOrderEventType eventType) { this.eventType = eventType; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
}
