package com.medical.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class InventoryProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryProcessorApplication.class, args);
    }
}
