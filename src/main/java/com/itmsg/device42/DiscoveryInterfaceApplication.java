package com.itmsg.device42;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DiscoveryInterfaceApplication {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(DiscoveryInterfaceApplication.class);
        springApplication.run(args);
    }
}
