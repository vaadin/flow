/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.webcomponent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.component.WebComponentExporterFactory.DefaultWebComponentExporterFactory;
import com.vaadin.flow.internal.ReflectTools;

/**
 * Internal utility methods for {@link WebComponentExporter} and
 * {@link WebComponentExporterFactory} classes.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 *
 */
public final class WebComponentExporterUtils {

    private WebComponentExporterUtils() {
        // Utility class should not be instantiated
    }

    /**
     * Returns exported web component factories based on exporters and factories
     * types.
     *
     * @param classes
     *            types of exporters and exporter factories
     * @return exported web component factories
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Set<WebComponentExporterFactory> getFactories(
            Set<Class<?>> classes) {
        Set<WebComponentExporterFactory> factories = new HashSet<>();
        classes.stream().filter(WebComponentExporter.class::isAssignableFrom)
                .filter(WebComponentExporterUtils::isEligible)
                .map(clazz -> new DefaultWebComponentExporterFactory(clazz))
                .forEach(factories::add);

        classes.stream()
                .filter(WebComponentExporterFactory.class::isAssignableFrom)
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                .filter(clazz -> !clazz
                        .equals(DefaultWebComponentExporterFactory.class))
                .map(ReflectTools::createInstance)
                .map(WebComponentExporterFactory.class::cast)
                .forEach(factories::add);
        return factories;
    }

    private static boolean isEligible(Class<?> clazz) {
        if (Modifier.isAbstract(clazz.getModifiers())) {
            getLogger().trace(
                    "Class {} is abstract, and won't be instantiated as a '{}' by default",
                    clazz.getName(),
                    WebComponentExporter.class.getSimpleName());
            return false;
        }
        if (!Modifier.isPublic(clazz.getModifiers())) {
            getLogger().trace(
                    "Class {} is not public, and won't be instantiated as a '{}' by default",
                    clazz.getName(),
                    WebComponentExporter.class.getSimpleName());
            return false;
        }
        Optional<Constructor<?>> constructor = Stream
                .of(clazz.getConstructors())
                .filter(ctor -> ctor.getParameterCount() == 0).findFirst();
        if (!constructor.isPresent()
                || !Modifier.isPublic(constructor.get().getModifiers())) {
            getLogger().warn(
                    "Class {} has no public no-argument constructor, and won't be instantiated as a '{}' by default",
                    clazz.getName(),
                    WebComponentExporter.class.getSimpleName());
            return false;
        }
        if (clazz.getEnclosingClass() != null
                && !Modifier.isStatic(clazz.getModifiers())) {
            getLogger().warn(
                    "Class {} is inner (nested non static) class, and won't be instantiated as a '{}' by default",
                    clazz.getName(),
                    WebComponentExporter.class.getSimpleName());
            return false;
        }
        return true;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(WebComponentExporterUtils.class);
    }
}
