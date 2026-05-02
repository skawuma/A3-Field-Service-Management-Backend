package com.a3solutions.fsm.workorder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 4/4/26
 */
@Schema(description = "Optional admin/dispatch reason for reopening a completed work order.")
public record ReopenWorkOrderRequest(
        @Schema(description = "Reason for reopening the work order.", example = "Return visit required after failed onsite validation")
        String reason
) {
}
