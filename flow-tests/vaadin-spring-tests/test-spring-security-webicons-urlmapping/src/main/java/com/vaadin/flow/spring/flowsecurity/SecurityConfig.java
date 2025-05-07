package com.vaadin.flow.spring.flowsecurity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;

import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;

@EnableWebSecurity
@Configuration
@Profile("default")
public class SecurityConfig {

    @Bean
    SecurityFilterChain vaadinSecurityFilterChain(HttpSecurity http)
            throws Exception {
        http.with(vaadin(), cfg -> cfg.loginView(LoginView.class));
        return http.build();
    }
}
