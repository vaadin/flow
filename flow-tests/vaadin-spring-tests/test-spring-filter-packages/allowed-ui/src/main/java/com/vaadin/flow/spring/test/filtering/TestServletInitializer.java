/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
