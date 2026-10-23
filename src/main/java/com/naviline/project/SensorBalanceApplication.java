package com.naviline.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SensorBalanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SensorBalanceApplication.class, args);
    }

}

