package pe.sga.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class MonitoringSecurityConfig {
    @Bean
    @Order(0)
    SecurityFilterChain localMonitoringChain(HttpSecurity http, Environment environment) throws Exception {
        http.securityMatcher(request -> {
            Integer configured = environment.getProperty("management.server.port", Integer.class);
            if (configured == null || configured < 0) return false;
            Integer actual = environment.getProperty("local.management.port", Integer.class, configured);
            Integer application = environment.getProperty("local.server.port", Integer.class);
            return actual > 0 && !actual.equals(application) && request.getLocalPort() == actual;
        });
        http.csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/prometheus").permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }
}
