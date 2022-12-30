/*
 * Copyright 2000-2022 Vaadin Ltd.
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
package com.vaadin.flow.component.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.internal.AbstractNavigationStateRenderer;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

/**
 * Tracks the location in source code where components were instantiated.
 *
 **/
public class ComponentTracker {

    private static Map<Component, StackTraceElement> createLocation = Collections
            .synchronizedMap(new WeakHashMap<>());
    private static Map<Component, StackTraceElement> attachLocation = Collections
            .synchronizedMap(new WeakHashMap<>());

    private static Boolean productionMode = null;
    private static String[] prefixesToSkip = new String[] {
            "com.vaadin.flow.component.", "com.vaadin.flow.di.",
            "com.vaadin.flow.dom.", "com.vaadin.flow.internal.",
            "com.vaadin.flow.spring.", "java.", "jdk.",
            "org.springframework.beans.", };

    /**
     * Finds the location where the given component instance was created.
     *
     * @param component
     *            the component to find
     * @return an element from the stack trace describing the relevant location
     *         where the component was created
     */
    public static StackTraceElement findCreate(Component component) {
        return createLocation.get(component);
    }

    /**
     * Tracks the location where the component was created. This should be
     * called from the Component constructor so that the creation location can
     * be found from the current stacktrace.
     *
     * @param component
     *            the component to track
     */
    public static void trackCreate(Component component) {
        if (isProductionMode()) {
            return;
        }
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        StackTraceElement location = findRelevantElement(component.getClass(),
                stack, null);
        if (isNavigatorCreate(location)) {
            location = findRelevantElement(null, stack, null);
        }
        createLocation.put(component, location);
    }

    /**
     * Finds the location where the given component instance was attached to a
     * parent.
     *
     * @param component
     *            the component to find
     * @return an element from the stack trace describing the relevant location
     *         where the component was attached
     */
    public static StackTraceElement findAttach(Component component) {
        return attachLocation.get(component);
    }

    /**
     * Tracks the location where the component was attached. This should be
     * called from the Component attach logic so that the creation location can
     * be found from the current stacktrace.
     *
     * @param component
     *            the component to track
     */
    public static void trackAttach(Component component) {
        if (isProductionMode()) {
            return;
        }
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        // In most cases the interesting attach call is found in the same class
        // where the component was created and not in a generic layout class
        StackTraceElement location = findRelevantElement(component.getClass(),
                stack, findCreate(component));
        if (isNavigatorCreate(location)) {
            // For routes, we can just show the init location as we have nothing
            // better
            location = createLocation.get(component);
        }
        attachLocation.put(component, location);
    }

    private static boolean isNavigatorCreate(StackTraceElement location) {
        return location.getClassName()
                .equals(AbstractNavigationStateRenderer.class.getName());
    }

    private static StackTraceElement findRelevantElement(
            Class<? extends Component> excludeClass, StackTraceElement[] stack,
            StackTraceElement preferredClass) {
        List<StackTraceElement> candidates = Stream.of(stack)
                .filter(e -> excludeClass == null
                        || !e.getClassName().equals(excludeClass.getName()))
                .filter(e -> {
                    for (String prefixToSkip : prefixesToSkip) {
                        if (e.getClassName().startsWith(prefixToSkip)) {
                            return false;
                        }
                    }
                    return true;
                }).collect(Collectors.toList());
        if (preferredClass != null) {
            Optional<StackTraceElement> preferredCandidate = candidates.stream()
                    .filter(e -> e.getClassName()
                            .equals(preferredClass.getClassName()))
                    .findFirst();
            if (preferredCandidate.isPresent()) {
                return preferredCandidate.get();
            }
        }
        return candidates.stream().findFirst().orElse(null);
    }

    /**
     * Checks if the application is running in production mode.
     *
     * When unsure, reports that production mode is true so tracking does not
     * take place in production.
     *
     * @return true if in production mode or the mode is unclear, false if in
     *         development mode
     **/
    private static boolean isProductionMode() {
        if (productionMode != null) {
            return productionMode;
        }

        VaadinService service = VaadinService.getCurrent();
        if (service == null) {
            // Rather fall back to not tracking if we are unsure, so we do not
            // use memory in production
            return true;
        }

        VaadinContext context = service.getContext();
        if (context == null) {
            return true;
        }
        ApplicationConfiguration applicationConfiguration = ApplicationConfiguration
                .get(context);
        if (applicationConfiguration == null) {
            return true;
        }

        productionMode = applicationConfiguration.isProductionMode();
        return productionMode;
    }

}
