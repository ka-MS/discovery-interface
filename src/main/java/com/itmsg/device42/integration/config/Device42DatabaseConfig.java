package com.itmsg.device42.integration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class Device42DatabaseConfig {
    private final Environment environment;

    public Device42DatabaseConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public Device42ConnectionFactory device42ConnectionFactory() throws ClassNotFoundException {
        Class.forName(environment.getProperty("device42.doql.jdbc.driver-class-name"));
        return new Device42ConnectionFactory(environment);
    }




}
