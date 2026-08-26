package com.itmsg.device42.integration.config;

import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Device42ConnectionFactory {
    private final Environment environment;

    public Device42ConnectionFactory(Environment environment) {
        this.environment = environment;
    }

    public Connection openConnection() throws SQLException {
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("user", environment.getProperty("device42.doql.jdbc.username"));
        connectionProperties.setProperty("password", environment.getProperty("device42.doql.jdbc.password"));

        return DriverManager.getConnection(environment.getProperty("device42.doql.jdbc.url"), connectionProperties);
    }

    public String schema() {
        return environment.getProperty("device42.doql.jdbc.schema");
    }
}
