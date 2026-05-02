package com.a3solutions.fsm.workorder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 11/19/25
 */
@Schema(description = "Payload used to assign a technician to a work order.")
public record AssignTechnicianRequest(
        @Schema(description = "Technician identifier to assign.", example = "3")
        Long technicianId
) {

}
