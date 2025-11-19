package com.a3solutions.fsm.exceptions;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.exceptions
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
public class BadRequestException  extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
