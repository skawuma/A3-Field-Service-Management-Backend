package com.a3solutions.fsm.workorder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 3/27/26
 */
@Schema(description = "Payload used when a technician signs off and completes a work order.")
public record CompleteWorkOrderRequest(
        @Schema(description = "Captured signature image as a data URL.", example = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...")
        String signatureDataUrl,
        @Schema(description = "Completion notes entered by the technician.", example = "Generator tested successfully and returned to service.")
        String completionNotes
) {
}
