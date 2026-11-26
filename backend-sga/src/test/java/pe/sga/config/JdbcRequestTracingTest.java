package pe.sga.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JdbcRequestTracingTest {
    @Test void realPoolQueryPreservesContextPrivacyAndMetricsType() throws Exception {
        Logger logger=(Logger)LoggerFactory.getLogger(JdbcRequestTracing.class);
        ListAppender<ILoggingEvent> events=new ListAppender<>();events.start();logger.addAppender(events);
        HikariConfig config=new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:jdbc_trace_test;DB_CLOSE_DELAY=-1");config.setMaximumPoolSize(1);
        try (HikariDataSource target=new HikariDataSource(config)) {
            Object bean=JdbcRequestTracing.jdbcRequestTracingPostProcessor().postProcessAfterInitialization(target,"dataSource");
            assertInstanceOf(HikariDataSource.class,bean);
            HikariDataSource pool=(HikariDataSource)bean;
            assertEquals(target.getHikariPoolMXBean(),pool.getHikariPoolMXBean());
            try (Connection connection=pool.getConnection()) {
                MDC.put("requestId","0123456789abcdef0123456789abcdef");
                try (var query=connection.prepareStatement("SELECT CAST(? AS VARCHAR)")) {
                    query.setString(1,"PRIVATE_CUSTOMER_VALUE");
                    try(var rows=query.executeQuery()) {assertTrue(rows.next());assertEquals("PRIVATE_CUSTOMER_VALUE",rows.getString(1));}
                    assertSame(connection,query.getConnection());
                }
                assertEquals(1,events.list.size());
                var event=events.list.getFirst();
                assertEquals(MDC.get("requestId"),event.getMDCPropertyMap().get("requestId"));
                assertTrue(event.getFormattedMessage().contains("db_system=h2"));
                assertTrue(event.getFormattedMessage().contains("outcome=SUCCESS"));
                assertFalse(event.getFormattedMessage().contains("SELECT"));
                assertFalse(event.getFormattedMessage().contains("PRIVATE_CUSTOMER_VALUE"));
                MDC.clear();
                try(var query=connection.createStatement();var rows=query.executeQuery("SELECT 1")) {assertTrue(rows.next());}
                assertEquals(1,events.list.size(),"No trace outside HTTP context");
            }
        } finally {MDC.clear();logger.detachAppender(events);events.stop();}
    }

    @Test void driverFailurePreservesExceptionAndHidesSqlAndParameters() throws Exception {
        Connection connection=mock(Connection.class);DatabaseMetaData metadata=mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(metadata);when(metadata.getDatabaseProductName()).thenReturn("MySQL");
        PreparedStatement statement=mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        SQLException failure=new SQLException("SELECT PRIVATE_PASSWORD");when(statement.executeQuery()).thenThrow(failure);
        Logger logger=(Logger)LoggerFactory.getLogger(JdbcRequestTracing.class);
        ListAppender<ILoggingEvent> events=new ListAppender<>();events.start();logger.addAppender(events);
        try {
            MDC.put("requestId","0123456789abcdef0123456789abcdef");
            var wrapped=JdbcRequestTracing.traceConnection(connection).prepareStatement("SELECT PRIVATE_PASSWORD");
            assertSame(failure,assertThrows(SQLException.class,wrapped::executeQuery));
            assertEquals(1,events.list.size());
            assertTrue(events.list.getFirst().getFormattedMessage().contains("db_system=mysql"));
            assertTrue(events.list.getFirst().getFormattedMessage().contains("outcome=ERROR"));
            assertFalse(events.list.getFirst().getFormattedMessage().contains("PRIVATE_PASSWORD"));
            assertNull(events.list.getFirst().getThrowableProxy());
        } finally {MDC.clear();logger.detachAppender(events);events.stop();}
    }
}
