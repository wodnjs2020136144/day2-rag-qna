package com.skala.day2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.skala.day2.config.Lab2RagProperties;

@SpringBootApplication
@EnableConfigurationProperties(Lab2RagProperties.class)
public class Day2Application {

    public static void main(String[] args) {
        SpringApplication.run(Day2Application.class, args);
    }
}
