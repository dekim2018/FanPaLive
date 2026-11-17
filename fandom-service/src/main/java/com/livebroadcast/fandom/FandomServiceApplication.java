package com.livebroadcast.fandom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.livebroadcast.fandom", "com.livebroadcast.common"})
@ConfigurationPropertiesScan("com.livebroadcast.fandom")
@EnableScheduling
public class FandomServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FandomServiceApplication.class, args);
    }
}
