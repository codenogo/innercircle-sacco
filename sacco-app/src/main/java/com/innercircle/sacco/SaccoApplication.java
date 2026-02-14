package com.innercircle.sacco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.innercircle.sacco")
public class SaccoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaccoApplication.class, args);
    }
}
