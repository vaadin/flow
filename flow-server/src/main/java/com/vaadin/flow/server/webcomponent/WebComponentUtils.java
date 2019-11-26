/*
 * Copyright 2000-2018 Vaadin Ltd.
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
 *
 * @author Vaadin Ltd
 *
 */
public final class WebComponentUtils {

    private WebComponentUtils() {
        // Utility class should not be instantiated
    }

    /**
     * Returns exported web component factories based on exporters and factories
     * types.
     *
     * @param classes
     *            types of exporters and exporter factories
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Set<WebComponentExporterFactory> getFactories(
            Set<Class<?>> classes) {
        Set<WebComponentExporterFactory> factories = new HashSet<>();
        classes.stream().filter(WebComponentExporter.class::isAssignableFrom)
                .filter(WebComponentUtils::isEligible)
                .map(clazz -> new DefaultWebComponentExporterFactory(clazz))
                .forEach(factories::add);

        classes.stream()
                .filter(WebComponentExporterFactory.class::isAssignableFrom)
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                .filter(clazz -> !clazz
                        .equals(DefaultWebComponentExporterFactory.class))
                .map(clazz -> ReflectTools.createInstance(clazz))
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
            getLogger().trace(
                    "Class {} has no public no-argument constructor, and won't be instantiated as a '{}' by default",
                    clazz.getName(),
                    WebComponentExporter.class.getSimpleName());
            return false;
        }
        if (clazz.getEnclosingClass() != null
                && !Modifier.isStatic(clazz.getModifiers())) {
            getLogger().trace(
                    "Class {} is inner (nested non static) class, and won't be instantiated as a '{}' by default",
                    clazz.getName(),
                    WebComponentExporter.class.getSimpleName());
            return false;
        }
        return true;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(WebComponentUtils.class);
    }
}
