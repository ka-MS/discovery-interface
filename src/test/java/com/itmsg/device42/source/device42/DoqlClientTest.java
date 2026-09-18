package com.itmsg.device42.source.device42;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DoqlClientTest {
    @Test
    void statementPathClosesResourcesInOriginalOrder() throws SQLException {
        var factory = mock(Device42ConnectionFactory.class);
        var connection = mock(Connection.class);
        var statement = mock(Statement.class);
        var result = mock(ResultSet.class);
        when(factory.openConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("page")).thenReturn(result);
        String value = new DoqlClient(factory).query("page", rs -> { rs.next(); return "read"; });
        assertThat(value).isEqualTo("read");
        var order = inOrder(factory, connection, statement, result);
        order.verify(factory).openConnection();
        order.verify(connection).createStatement();
        order.verify(statement).executeQuery("page");
        order.verify(result).next();
        order.verify(result).close();
        order.verify(statement).close();
        order.verify(connection).close();
        order.verifyNoMoreInteractions();
    }

    @Test
    void preparedPathPreservesReaderFailureAndSuppressedCloseFailure() throws SQLException {
        var factory = mock(Device42ConnectionFactory.class);
        var connection = mock(Connection.class);
        var statement = mock(PreparedStatement.class);
        var result = mock(ResultSet.class);
        when(factory.openConnection()).thenReturn(connection);
        when(connection.prepareStatement("count")).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        var readFailure = new SQLException("read");
        var closeFailure = new SQLException("close");
        doThrow(closeFailure).when(result).close();
        assertThatThrownBy(() -> new DoqlClient(factory).preparedQuery("count", rs -> { throw readFailure; }))
                .isSameAs(readFailure);
        assertThat(readFailure.getSuppressed()).containsExactly(closeFailure);
        verify(statement).close();
        verify(connection).close();
        verify(connection, never()).createStatement();
    }

    @Test
    void acquisitionFailureDoesNotIntroduceAnotherRetryOrExceptionWrapper() {
        var factory = mock(Device42ConnectionFactory.class);
        var failure = new IllegalStateException("connection failed");
        when(factory.openConnection()).thenThrow(failure);
        assertThatThrownBy(() -> new DoqlClient(factory).query("page", rs -> null)).isSameAs(failure);
        verify(factory, times(1)).openConnection();
    }
}
