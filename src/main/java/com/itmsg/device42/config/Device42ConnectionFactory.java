package com.itmsg.device42.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Device42ConnectionFactory {

    private static final Logger log = LoggerFactory.getLogger(Device42ConnectionFactory.class);
    private static final int MAX_RETRY_COUNT = 3;
    private static final long RETRY_DELAY_MS = 2_000L;
    private static final int VALIDATION_TIMEOUT_SECONDS = 3;

    private final String url;
    private final Properties connectionProperties;

    public Device42ConnectionFactory(Environment environment) {
        this.url = environment.getRequiredProperty("device42.doql.jdbc.url");
        this.connectionProperties = new Properties();
        this.connectionProperties.setProperty(
                "user",
                environment.getRequiredProperty("device42.doql.jdbc.username")
        );
        this.connectionProperties.setProperty(
                "password",
                environment.getRequiredProperty("device42.doql.jdbc.password")
        );
    }

    public Connection openConnection() {
        SQLException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            Connection connection = null;

            try {
                connection = DriverManager.getConnection(url, connectionProperties);
                validate(connection);

                log.info("Device42 데이터베이스 연결에 성공했습니다. attempt={}", attempt);
                return connection;
            } catch (SQLException e) {
                lastException = e;
                closeQuietly(connection);

                log.warn(
                        "Device42 데이터베이스 연결에 실패했습니다. attempt={}/{}",
                        attempt,
                        MAX_RETRY_COUNT,
                        e
                );

                if (attempt < MAX_RETRY_COUNT) {
                    waitBeforeRetry();
                }
            }
        }

        throw new IllegalStateException(
                "Device42 데이터베이스 연결에 %d회 실패했습니다.".formatted(MAX_RETRY_COUNT),
                lastException
        );
    }

    private void validate(Connection connection) throws SQLException {
        if (connection == null) {
            throw new SQLException("JDBC 드라이버가 null 커넥션을 반환했습니다.");
        }

//        if (!connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
//            throw new SQLException("Device42 데이터베이스 커넥션이 유효하지 않습니다.");
//        }
    }

    private void waitBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Device42 데이터베이스 연결 재시도가 중단되었습니다.", e);
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException e) {
            log.debug("유효하지 않은 Device42 데이터베이스 커넥션을 닫지 못했습니다.", e);
        }
    }
}
