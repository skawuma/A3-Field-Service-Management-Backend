package com.a3solutions.fsm.workorder;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 3/27/26
 */
public record CompleteWorkOrderRequest(        String signatureDataUrl,
                                               String completionNotes) {
}
