package com.clearfund;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ClearFundApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClearFundApplication.class, args);
    }
}
