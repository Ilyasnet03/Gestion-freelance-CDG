package com.cdg.freelance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FreelanceManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreelanceManagementApplication.class, args);
    }
}
