package com.a3solutions.fsm.config.demo;

import com.a3solutions.fsm.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoModeGuardTest {

    @Test
    void blocksPermanentDeletionInDemoMode() {
        DemoModeGuard guard = new DemoModeGuard(true);

        assertThatThrownBy(() -> guard.rejectPermanentDeletion("technicians"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("disabled in the public demo");
    }

    @Test
    void allowsPermanentDeletionOutsideDemoMode() {
        DemoModeGuard guard = new DemoModeGuard(false);

        assertThatCode(() -> guard.rejectPermanentDeletion("technicians"))
                .doesNotThrowAnyException();
    }
}
