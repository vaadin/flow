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
package com.vaadin.flow.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.VaadinApplicationConfiguration;
import com.vaadin.flow.spring.VaadinScanPackagesRegistrar;
import com.vaadin.flow.spring.VaadinScopesConfig;
import com.vaadin.flow.spring.VaadinServletConfiguration;

/**
 * Brings in the machinery to setup Spring + Vaadin applications. This
 * annotation should be added on a {@link Configuration} class of the
 * application to automatically import Vaadin configuration (such as
 * {@link VaadinScopesConfig}).
 * <p>
 * Use this annotation in your Spring Boot application to scan the packages with
 * Vaadin types that should be discovered at startup (e.g. routes
 * via @{@link Route} annotation).
 * <p>
 * You don't need this annotation if your application runs as a Web application
 * being deployed into a Web container. But if you run your application as a
 * Spring Boot application then classpath scanning is disabled due the Spring
 * Boot design (see <a href=
 * "https://github.com/spring-projects/spring-boot/issues/321">ServletContainerInitializers
 * issue</a>). Spring Vaadin add-on implements this scanning for you but it uses
 * the default application package for this (the package where you have your
 * Spring Boot application class). It means that if your Vaadin classes are
 * inside this package or its descendant subpackage then everything works out of
 * the box. Otherwise you should use {@link EnableVaadin} annotation with
 * package names to scan at startup as a value.
 *
 * @see <a href=
 *      "https://github.com/spring-projects/spring-boot/issues/321">ServletContainerInitializers
 *      issue</a>
 *
 * @author Vaadin Ltd
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({ VaadinScopesConfig.class, VaadinServletConfiguration.class,
        VaadinScanPackagesRegistrar.class,
        VaadinApplicationConfiguration.class })
public @interface EnableVaadin {

    /**
     * Base packages to scan for annotated classes on Vaadin startup.
     * <p>
     * If packages are not specified then default Spring Boot application
     * package is used.
     *
     * @return the base packages to scan
     */
    String[] value() default {};
}
