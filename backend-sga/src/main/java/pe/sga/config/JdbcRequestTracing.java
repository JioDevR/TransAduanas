package pe.sga.config;

import com.zaxxer.hikari.HikariDataSource;
import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/** Correlates completed JDBC operations without SQL text, bind values or results. */
@Configuration(proxyBeanMethods = false)
public class JdbcRequestTracing {
    private static final Logger LOG = LoggerFactory.getLogger(JdbcRequestTracing.class);

    @Bean
    static BeanPostProcessor jdbcRequestTracingPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String name) {
                if (!(bean instanceof HikariDataSource)) return bean;
                // Keep the concrete pool type, lifecycle and metrics available to Spring Boot.
                ProxyFactory proxy = new ProxyFactory(bean);
                proxy.setProxyTargetClass(true);
                proxy.addAdvice((MethodInterceptor) invocation -> {
                    Object result = invocation.proceed();
                    if (invocation.getMethod().getName().equals("getConnection") && result instanceof Connection connection) {
                        return traceConnection(connection);
                    }
                    return result;
                });
                return proxy.getProxy();
            }
        };
    }

    static Connection traceConnection(Connection connection) {
        String product;
        try {
            String name = connection.getMetaData().getDatabaseProductName();
            product = "MySQL".equalsIgnoreCase(name) ? "mysql" : "H2".equalsIgnoreCase(name) ? "h2" : "other";
        } catch (SQLException ex) {
            product = "unknown";
        }
        String dbSystem = product;
        return (Connection) Proxy.newProxyInstance(JdbcRequestTracing.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    Object result = invoke(connection, method, args);
                    if (result instanceof Statement statement &&
                            (method.getName().equals("createStatement") || method.getName().equals("prepareStatement") || method.getName().equals("prepareCall"))) {
                        Class<?> type = statement instanceof CallableStatement ? CallableStatement.class :
                                statement instanceof PreparedStatement ? PreparedStatement.class : Statement.class;
                        return Proxy.newProxyInstance(JdbcRequestTracing.class.getClassLoader(), new Class<?>[]{type},
                                (statementProxy, statementMethod, statementArgs) -> {
                                    // getConnection must retain tracing for statements created subsequently.
                                    if (statementMethod.getName().equals("getConnection")) return proxy;
                                    if (!statementMethod.getName().startsWith("execute")) return invoke(statement, statementMethod, statementArgs);
                                    String requestId = MDC.get("requestId");
                                    if (requestId == null || !requestId.matches("[a-f0-9]{32}")) return invoke(statement, statementMethod, statementArgs);
                                    String spanId = UUID.randomUUID().toString().replace("-", "");
                                    long start = System.nanoTime();
                                    try {
                                        Object executed = invoke(statement, statementMethod, statementArgs);
                                        LOG.info("jdbc_query_completed db_system={} jdbc_span_id={} operation={} outcome=SUCCESS duration_ms={}",
                                                dbSystem, spanId, statementMethod.getName(), (System.nanoTime()-start)/1_000_000.0);
                                        return executed;
                                    } catch (Throwable error) {
                                        // Do not include exception messages: drivers may embed SQL or values.
                                        LOG.warn("jdbc_query_completed db_system={} jdbc_span_id={} operation={} outcome=ERROR duration_ms={}",
                                                dbSystem, spanId, statementMethod.getName(), (System.nanoTime()-start)/1_000_000.0);
                                        throw error;
                                    }
                                });
                    }
                    return result;
                });
    }

    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }
}
