package com.weeds.findsoul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author weeds
 */
@SpringBootApplication
@EnableScheduling
public class FindSoulApplication {
    public static void main(String[] args) {
        SpringApplication.run(FindSoulApplication.class, args);
    }
}
