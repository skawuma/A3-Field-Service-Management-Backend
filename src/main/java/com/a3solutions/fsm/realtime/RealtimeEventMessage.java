package com.a3solutions.fsm.realtime;

import java.time.Instant;
import java.util.Map;
/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.realtime
 * @project A3 Field Service Management Backend
 * @date 4/14/26
 */
public class RealtimeEventMessage {

    private RealtimeEventType type;
    private String message;
    private Long workOrderId;
    private Long technicianId;
    private String status;
    private Instant timestamp;
    private Map<String, Object> metadata;

    public RealtimeEventMessage() {
    }

    public RealtimeEventMessage(
            RealtimeEventType type,
            String message,
            Long workOrderId,
            Long technicianId,
            String status,
            Instant timestamp,
            Map<String, Object> metadata
    ) {
        this.type = type;
        this.message = message;
        this.workOrderId = workOrderId;
        this.technicianId = technicianId;
        this.status = status;
        this.timestamp = timestamp;
        this.metadata = metadata;
    }

    public static RealtimeEventMessage of(
            RealtimeEventType type,
            String message,
            Long workOrderId,
            Long technicianId,
            String status,
            Map<String, Object> metadata
    ) {
        return new RealtimeEventMessage(
                type,
                message,
                workOrderId,
                technicianId,
                status,
                Instant.now(),
                metadata
        );
    }

    public RealtimeEventType getType() {
        return type;
    }

    public void setType(RealtimeEventType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public Long getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

}
