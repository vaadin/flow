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
package com.vaadin.flow.server.startup;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.InvalidApplicationConfigurationException;

/**
 * Validation class that contains common logic to checks that specific
 * annotations are not configured wrong.
 *
 * @since 1.0
 */
public abstract class AbstractAnnotationValidator implements Serializable {

    public static final String ERROR_MESSAGE_BEGINNING = "Found configuration annotations that will not be used in the application. \n"
            + "Move the following annotations to a single route or the top RouterLayout of the application: \n";

    public static final String NON_PARENT = "Non parent Route target: %s contains: %s";
    public static final String NON_PARENT_ALIAS = "Non parent RouteAlias target: %s contains: %s";
    public static final String NON_ROUTER_LAYOUT = "Non RouterLayout: %s contains: %s";
    public static final String MIDDLE_ROUTER_LAYOUT = "Middle layout: %s contains: %s";

    /**
     * Validate the correctness of the annotations returned by the
     * {@link #getAnnotations()} method applied to the {@code classSet}.
     *
     * @param classSet
     *            the classes to validate
     */
    protected void validateClasses(Collection<Class<?>> classSet) {
        if (classSet == null) {
            return;
        }

        List<String> offendingAnnotations = validateAnnotatedClasses(classSet);

        if (!offendingAnnotations.isEmpty()) {
            String message = getErrorHint()
                    + String.join("\n", offendingAnnotations);
            throw new InvalidApplicationConfigurationException(message);
        }
    }

    /**
     * Gets the annotations that are subject to validate.
     *
     * @return a list of target annotations
     */
    protected abstract List<Class<?>> getAnnotations();

    /**
     * Handles the {@code clazz} which is not a top level route and not a router
     * layout. Returns an optional message which describes the error having an
     * annotation for the class.
     *
     * @param clazz
     *            class to validate annotations
     * @return an optional error message or empty if there is no error
     */
    protected Optional<String> handleNonRouterLayout(Class<?> clazz) {
        return Optional.of(String.format(NON_ROUTER_LAYOUT, clazz.getName(),
                getClassAnnotations(clazz)));
    }

    /**
     * Returns a hint for the discovered validation errors.
     *
     * @return the error hint
     */
    protected String getErrorHint() {
        return ERROR_MESSAGE_BEGINNING;
    }

    /**
     * Returns the validation annotations declared for the {@code clazz}.
     *
     * @param clazz
     *            the type to get validation annotations
     * @return comma separated list of validation annotation declared for the
     *         {@code clazz}
     */
    protected String getClassAnnotations(Class<?> clazz) {
        return getAnnotations().stream()
                .filter(ann -> clazz
                        .isAnnotationPresent((Class<? extends Annotation>) ann))
                .map(Class::getSimpleName).collect(Collectors.joining(", "));
    }

    private List<String> validateAnnotatedClasses(
            Collection<Class<?>> classSet) {
        List<String> offendingAnnotations = new ArrayList<>();

        for (Class<?> clazz : classSet) {
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
                if (!Modifier.isAbstract(clazz.getModifiers())) {
                    handleNonRouterLayout(clazz)
                            .ifPresent(offendingAnnotations::add);
                }
            } else if (RouterLayout.class.isAssignableFrom(clazz)
                    && clazz.getAnnotation(ParentLayout.class) != null) {
                offendingAnnotations.add(String.format(MIDDLE_ROUTER_LAYOUT,
                        clazz.getName(), getClassAnnotations(clazz)));
            }
        }

        return offendingAnnotations;
    }

}
