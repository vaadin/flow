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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.internal.AbstractNavigationStateRenderer;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

/**
 * Tracks the location in source code where components were instantiated.
 **/
public class ComponentTracker {

    private static final Map<Component, Location> createLocation = Collections
            .synchronizedMap(new WeakHashMap<>());
    private static final Map<Component, Location> attachLocation = Collections
            .synchronizedMap(new WeakHashMap<>());

    private static Boolean disabled = null;
    private static final String[] prefixesToSkip = new String[] {
            "com.vaadin.flow.component.", "com.vaadin.flow.di.",
            "com.vaadin.flow.dom.", "com.vaadin.flow.internal.",
            "com.vaadin.flow.spring.", "java.", "jdk.",
            "org.springframework.beans.", };

    /**
     * Represents a location in the source code.
     */
    public static class Location implements Serializable {
        private final String className;
        private final String filename;
        private final String methodName;
        private final int lineNumber;

        public Location(String className, String filename, String methodName,
                int lineNumber) {
            this.className = className;
            this.filename = filename;
            this.methodName = methodName;
            this.lineNumber = lineNumber;
        }

        public String className() {
            return className;
        }

        public String filename() {
            return filename;
        }

        public String methodName() {
            return methodName;
        }

        public int lineNumber() {
            return lineNumber;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            Location location = (Location) o;

            if (lineNumber != location.lineNumber)
                return false;
            if (!Objects.equals(className, location.className))
                return false;
            if (!Objects.equals(filename, location.filename))
                return false;
            return Objects.equals(methodName, location.methodName);
        }

        @Override
        public int hashCode() {
            int result = className != null ? className.hashCode() : 0;
            result = 31 * result + (filename != null ? filename.hashCode() : 0);
            result = 31 * result
                    + (methodName != null ? methodName.hashCode() : 0);
            result = 31 * result + lineNumber;
            return result;
        }

        @Override
        public String toString() {
            return "Component '" + className + "' at '" + filename + "' ("
                    + methodName + " LINE " + lineNumber + ")";
        }
    }

    /**
     * Finds the location where the given component instance was created.
     *
     * @param component
     *            the component to find
     * @return the location where the component was created
     */
    public static Location findCreate(Component component) {
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
        if (isDisabled()) {
            return;
        }
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        Location[] relevantLocations = findRelevantLocations(stack);
        Location location = findRelevantLocation(component.getClass(),
                relevantLocations, null);
        if (isNavigatorCreate(location)) {
            location = findRelevantLocation(null, relevantLocations, null);
        }
        createLocation.put(component, location);
    }

    /**
     * Finds the location where the given component instance was attached to a
     * parent.
     *
     * @param component
     *            the component to find
     * @return the location where the component was attached
     */
    public static Location findAttach(Component component) {
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
        if (isDisabled()) {
            return;
        }
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        // In most cases the interesting attach call is found in the same class
        // where the component was created and not in a generic layout class
        Location[] relevantLocations = findRelevantLocations(stack);
        Location location = findRelevantLocation(component.getClass(),
                relevantLocations, findCreate(component));
        if (isNavigatorCreate(location)) {
            // For routes, we can just show the init location as we have nothing
            // better
            location = createLocation.get(component);
        }
        attachLocation.put(component, location);
    }

    private static Location[] findRelevantLocations(StackTraceElement[] stack) {
        return Stream.of(stack).filter(e -> {
            for (String prefixToSkip : prefixesToSkip) {
                if (e.getClassName().startsWith(prefixToSkip)) {
                    return false;
                }
            }
            return true;
        }).map(ComponentTracker::toLocation).toArray(Location[]::new);
    }

    private static Location findRelevantLocation(
            Class<? extends Component> excludeClass, Location[] locations,
            Location preferredClass) {
        List<Location> candidates = Arrays.stream(locations)
                .filter(location -> excludeClass == null
                        || !location.className().equals(excludeClass.getName()))
                .filter(location -> {
                    for (String prefixToSkip : prefixesToSkip) {
                        if (location.className().startsWith(prefixToSkip)) {
                            return false;
                        }
                    }
                    return true;
                }).collect(Collectors.toList());
        if (preferredClass != null) {
            Optional<Location> preferredCandidate = candidates.stream()
                    .filter(location -> location.className()
                            .equals(preferredClass.className()))
                    .findFirst();
            if (preferredCandidate.isPresent()) {
                return preferredCandidate.get();
            }
        }
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static boolean isNavigatorCreate(Location location) {
        return location.className()
                .equals(AbstractNavigationStateRenderer.class.getName());
    }

    /**
     * Checks if the component tracking is disabled.
     * <p>
     * Tracking is disabled when application is running in production mode or if
     * the configuration property
     * {@literal vaadin.devmode.componentTracker.enabled} is set to
     * {@literal false}.
     * <p>
     * When unsure, reports that production mode is true so tracking does not
     * take place in production.
     *
     * @return true if in production mode or the mode is unclear, false if in
     *         development mode
     **/
    private static boolean isDisabled() {
        if (disabled != null) {
            return disabled;
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

        DeploymentConfiguration configuration = service
                .getDeploymentConfiguration();
        if (configuration == null) {
            return true;
        }

        disabled = configuration.isProductionMode()
                || !configuration.getBooleanProperty(
                        InitParameters.APPLICATION_PARAMETER_DEVMODE_ENABLE_COMPONENT_TRACKER,
                        true);
        return disabled;
    }

    private static Location toLocation(StackTraceElement stackTraceElement) {
        if (stackTraceElement == null) {
            return null;
        }

        String className = stackTraceElement.getClassName();
        String fileName = stackTraceElement.getFileName();
        String methodName = stackTraceElement.getMethodName();
        int lineNumber = stackTraceElement.getLineNumber();
        return new Location(className, fileName, methodName, lineNumber);
    }

}
