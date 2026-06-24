/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test.filtering;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vaadin.flow.spring.VaadinServletContextInitializer;

/**
 * The entry point of the Spring Boot application.
 */
@SpringBootApplication
@Configuration
public class TestServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(TestServletInitializer.class, args);
    }

    @Bean
    public VaadinServletContextInitializer vaadinServletContextInitializer(
            ApplicationContext context) {

        return new VaadinServletContextInitializer(context) {
            @Override
            protected Set<Class<?>> findClassesForDevMode(
                    Set<String> basePackages,
                    List<Class<? extends Annotation>> annotations,
                    List<Class<?>> superTypes) {
                ClassScannerView.classes = super.findClassesForDevMode(
                        basePackages, annotations, superTypes);
                return ClassScannerView.classes;
            }
        };
    }
}
