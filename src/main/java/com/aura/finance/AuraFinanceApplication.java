package com.aura.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AuraFinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuraFinanceApplication.class, args);
    }
}
