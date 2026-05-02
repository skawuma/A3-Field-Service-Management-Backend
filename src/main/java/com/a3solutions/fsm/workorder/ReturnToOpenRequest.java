package com.a3solutions.fsm.workorder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 4/5/26
 */
@Schema(description = "Optional reason provided when a technician returns a work order to OPEN.")
public record ReturnToOpenRequest(
        @Schema(description = "Reason the technician cannot proceed and needs reassignment.", example = "Customer site inaccessible until tomorrow")
        String reason
) {
}
