package com.a3solutions.fsm.config.demo;

import com.a3solutions.fsm.exceptions.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DemoModeGuard {

    private final boolean demoMode;

    public DemoModeGuard(@Value("${app.demo.mode:false}") boolean demoMode) {
        this.demoMode = demoMode;
    }

    public void rejectPermanentDeletion(String resourceName) {
        if (demoMode) {
            throw new BusinessRuleException(
                    "Permanent deletion of " + resourceName + " is disabled in the public demo."
            );
        }
    }
}
