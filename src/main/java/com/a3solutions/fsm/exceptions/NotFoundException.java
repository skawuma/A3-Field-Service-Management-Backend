package com.a3solutions.fsm.exceptions;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.exceptions
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
