package com.a3solutions.fsm.config.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
@ConfigurationProperties(prefix = "app.demo")
public class DemoDataProperties {

    private boolean mode = true;
    private boolean dataEnabled = true;
    private DemoAccount admin = new DemoAccount("admin.demo@a3fsm.com", "DemoAdmin2026!");
    private DemoAccount dispatcher = new DemoAccount("dispatcher.demo@a3fsm.com", "DemoDispatch2026!");
    private DemoAccount technician = new DemoAccount("tech.demo@a3fsm.com", "DemoTech2026!");

    public boolean isMode() {
        return mode;
    }

    public void setMode(boolean mode) {
        this.mode = mode;
    }

    public boolean isDataEnabled() {
        return dataEnabled;
    }

    public void setDataEnabled(boolean dataEnabled) {
        this.dataEnabled = dataEnabled;
    }

    public DemoAccount getAdmin() {
        return admin;
    }

    public void setAdmin(DemoAccount admin) {
        this.admin = admin;
    }

    public DemoAccount getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(DemoAccount dispatcher) {
        this.dispatcher = dispatcher;
    }

    public DemoAccount getTechnician() {
        return technician;
    }

    public void setTechnician(DemoAccount technician) {
        this.technician = technician;
    }

    public static class DemoAccount {
        private String email;
        private String password;

        public DemoAccount() {
        }

        public DemoAccount(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
