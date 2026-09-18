package com.itmsg.device42.source.device42;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.springframework.stereotype.Component;

/** JDBC 자원의 수명만 관리한다. SQL, 행 변환, 오류 처리 정책은 호출자가 소유한다. */
@Component
public class DoqlClient {
    private final Device42ConnectionFactory connectionFactory;

    public DoqlClient(Device42ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public <T> T query(String sql, ResultReader<T> reader) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return reader.read(resultSet);
        }
    }

    /** 기존 COUNT의 PreparedStatement 실행 경로를 유지한다. */
    public <T> T preparedQuery(String sql, ResultReader<T> reader) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return reader.read(resultSet);
        }
    }

    @FunctionalInterface
    public interface ResultReader<T> {
        T read(ResultSet resultSet) throws SQLException;
    }
}
