package com.tableorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TableOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(TableOrderApplication.class, args);
    }
}
