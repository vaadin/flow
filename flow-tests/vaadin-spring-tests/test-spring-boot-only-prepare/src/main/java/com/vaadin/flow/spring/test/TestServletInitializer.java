/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import com.vaadin.flow.router.internal.ClientRoutesProvider;

@SpringBootApplication
@Configuration
@EnableWebSecurity
@Import(DummyOAuth2Server.class)
public class TestServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(TestServletInitializer.class, args);
    }

    @Bean
    public ClientRoutesProvider hillaClientRoutesProvider() {
        return new ClientRoutesProvider() {
            @Override
            public List<String> getClientRoutes() {
                return List.of("$index", "$layout", "/hilla",
                        "/hilla/person/:id", "/hilla/persons/:id?");
            }
        };
    }

    @Bean
    public ClientRoutesProvider anotherhillaClientRoutesProvider() {
        return new ClientRoutesProvider() {
            @Override
            public List<String> getClientRoutes() {
                return List.of("/$layout", "/hilla", "/hilla/hilla/$index",
                        "/anotherhilla");
            }
        };
    }
}
