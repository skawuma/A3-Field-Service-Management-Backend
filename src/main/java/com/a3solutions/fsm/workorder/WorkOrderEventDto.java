package com.a3solutions.fsm.workorder;

import java.time.Instant;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3_SOLUTIONS_PROJECT
 * @date 11/26/25
 */
public class WorkOrderEventDto {


    private Long id;
    private WorkOrderEventType eventType;
    private Instant createdAt;
    private String message;
    private String oldValue;
    private String newValue;
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
