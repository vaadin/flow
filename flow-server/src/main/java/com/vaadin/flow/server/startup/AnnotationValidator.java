/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.server.startup;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.InvalidApplicationConfigurationException;
import com.vaadin.flow.server.Theme;

@HandlesTypes({ Viewport.class, BodySize.class, Inline.class, Theme.class })
public class AnnotationValidator implements ServletContainerInitializer {

    public static final String ERROR_MESSAGE_BEGINNING = "Found configuration annotations that will not be used in the application. \n"
            + "Move the following annotations to a single route or the top RouterLayout of the application: \n";

    public static final String NON_PARENT = "Non parent Route target: %s contains: %s";
    public static final String NON_PARENT_ALIAS = "Non parent RouteAlias target: %s contains: %s";
    public static final String NON_ROUTER_LAYOUT = "Non RouterLayout: %s contains: %s";
    public static final String MIDDLE_ROUTER_LAYOUT = "Middle layout: %s contains: %s";

    private List<Class<?>> typeAnnotations;

    @Override
    public void onStartup(Set<Class<?>> classSet, ServletContext servletContext)
            throws ServletException {
        if (classSet == null) {
            return;
        }

        // Get the handled annotations
        typeAnnotations = Arrays.asList(
                this.getClass().getAnnotation(HandlesTypes.class).value());

        List<String> offendingAnnotations = validateAnnotatedClasses(classSet);

        if (!offendingAnnotations.isEmpty()) {
            String message = ERROR_MESSAGE_BEGINNING
                    + String.join("\n", offendingAnnotations);
            throw new InvalidApplicationConfigurationException(message);
        }
    }

    private List<String> validateAnnotatedClasses(Set<Class<?>> classSet) {
        List<String> offendingAnnotations = new ArrayList<>();

        classSet.forEach(clazz -> {
            Route route = clazz.getAnnotation(Route.class);
            if (route != null) {
                if (!UI.class.equals(route.layout())) {
                    offendingAnnotations.add(String.format(NON_PARENT,
                            clazz.getName(), getClassAnnotations(clazz)));
                }
                RouteAlias routeAlias = clazz.getAnnotation(RouteAlias.class);
                if (routeAlias != null
                        && !UI.class.equals(routeAlias.layout())) {
                    offendingAnnotations.add(String.format(NON_PARENT_ALIAS,
                            clazz.getName(), getClassAnnotations(clazz)));
                }
            } else if (!RouterLayout.class.isAssignableFrom(clazz)) {
                offendingAnnotations.add(String.format(NON_ROUTER_LAYOUT,
                        clazz.getName(), getClassAnnotations(clazz)));
            } else if (RouterLayout.class.isAssignableFrom(clazz)
                    && clazz.getAnnotation(ParentLayout.class) != null) {
                offendingAnnotations.add(String.format(MIDDLE_ROUTER_LAYOUT,
                        clazz.getName(), getClassAnnotations(clazz)));
            }
        });

        return offendingAnnotations;
    }

    private String getClassAnnotations(Class<?> clazz) {
        List<String> faultyAnnotations = Stream.of(clazz.getAnnotations())
                .map(Annotation::annotationType)
                .filter(typeAnnotations::contains).map(Class::getSimpleName)
                .collect(Collectors.toList());
        return String.join(", ", faultyAnnotations);
    }
}
