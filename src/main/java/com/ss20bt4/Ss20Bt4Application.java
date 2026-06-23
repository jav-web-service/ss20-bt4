package com.ss20bt4;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class Ss20Bt4Application {

    public static void main(String[] args) {
        SpringApplication.run(Ss20Bt4Application.class, args);
    }

}
