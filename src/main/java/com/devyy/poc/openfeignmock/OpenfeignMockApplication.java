package com.devyy.poc.openfeignmock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients("com.devyy.poc.openfeignmock.feign")
@SpringBootApplication
public class OpenfeignMockApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpenfeignMockApplication.class, args);
    }
}