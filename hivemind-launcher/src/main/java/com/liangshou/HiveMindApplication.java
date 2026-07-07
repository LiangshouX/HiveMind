package com.liangshou;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.liangshou")
@MapperScan({"com.liangshou.infrastructure.datasource.mapper", "com.liangshou.agentic.infrastructure.mysql.mapper"})
public class HiveMindApplication {
    public static void main(String[] args) {
        SpringApplication.run(HiveMindApplication.class, args);
    }
}

