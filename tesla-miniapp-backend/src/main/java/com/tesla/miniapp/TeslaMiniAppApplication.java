package com.tesla.miniapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Tesla MiniApp Backend Application
 * 
 * @author Tesla MiniApp Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class TeslaMiniAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeslaMiniAppApplication.class, args);
    }
}