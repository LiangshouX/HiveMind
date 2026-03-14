package com.liangshou;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan
public class TangApplication {
    public static void main(String[] args) {
        SpringApplication.run(TangApplication.class, args);
    }

}

