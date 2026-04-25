package com.a3solutions.fsm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class A3FieldServiceManagementBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(A3FieldServiceManagementBackendApplication.class, args);
    }

}
