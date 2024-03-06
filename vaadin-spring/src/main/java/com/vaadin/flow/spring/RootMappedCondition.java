/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import java.lang.reflect.InvocationTargetException;

import com.vaadin.flow.server.VaadinServlet;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Condition to check whether the Vaadin servlet is mapped to the root
 * ({@literal "/*"}).
 * <p>
 * In this case a {@link DispatcherServlet} is used. It's mapped to the root
 * instead of VaadinServlet and forwards requests to {@link VaadinServlet}. If
 * there are other mappings (via Spring endpoints e.g.) then
 * {@link DispatcherServlet} makes it possible to handle them properly via those
 * endpoints. Otherwise {@link VaadinServlet} will handle all the URLs because
 * it has the highest priority.
 *
 * @author Vaadin Ltd
 *
 */
public class RootMappedCondition implements Condition {

    public static final String URL_MAPPING_PROPERTY = "vaadin.urlMapping";

    @Override
    public boolean matches(ConditionContext context,
            AnnotatedTypeMetadata metadata) {

        String urlMapping = getUrlMapping(context.getEnvironment());
        return isRootMapping(urlMapping);
    }

    /**
     * Gets the url mapping in a way compatible with both plain Spring and
     * Spring Boot.
     *
     * @param environment
     *            the application environment
     * @return the url mapping or null if none is defined
     */
    public static String getUrlMapping(Environment environment) {
        if (SpringUtil.isSpringBoot()) {
            try {
                return (String) Class.forName(
                        "com.vaadin.flow.spring.VaadinConfigurationProperties")
                        .getMethod("getUrlMapping", Environment.class)
                        .invoke(null, environment);
            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException
                    | SecurityException | ClassNotFoundException e) {
                LoggerFactory.getLogger(RootMappedCondition.class)
                        .error("Unable to find urlMapping from properties", e);
                return null;
            }
        } else {
            return environment.getProperty(URL_MAPPING_PROPERTY);
        }
    }

    /**
     * Returns {@code true} if {@code mapping} is the root mapping
     * ({@literal "/*"}).
     * <p>
     * The mapping is controlled via the {@code vaadin.urlMapping} property
     * value. By default it's {@literal "/*"}.
     *
     * @param mapping
     *            the mapping string to check
     * @return {@code true} if {@code mapping} is the root mapping and
     *         {@code false} otherwise
     */
    public static boolean isRootMapping(String mapping) {
        if (mapping == null) {
            return true;
        }
        return mapping.trim().replaceAll("(/\\**)?$", "").isEmpty();
    }
}
