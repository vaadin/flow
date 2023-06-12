/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.internal.AbstractNavigationStateRenderer;
import com.vaadin.flow.server.AbstractConfiguration;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

/**
 * Tracks the location in source code where components were instantiated.
 *
 **/
public class ComponentTracker {

    private static Map<Component, Location> createLocation = Collections
            .synchronizedMap(new WeakHashMap<>());
    private static Map<Component, Location> attachLocation = Collections
            .synchronizedMap(new WeakHashMap<>());

    private static Boolean productionMode = null;
    private static String[] prefixesToSkip = new String[] {
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

        /**
         * Finds the Java file this location refers to.
         *
         * @param configuration
         *            the application configuration
         * @return the Java file the location refers to, or {@code null}
         */
        public File findJavaFile(AbstractConfiguration configuration) {
            String cls = className();
            String filename = filename();
            if (!cls.endsWith(filename.replace(".java", ""))) {
                return null;
            }
            File src = configuration.getJavaSourceFolder();
            File javaFile = new File(src,
                    cls.replace(".", File.separator) + ".java");
            return javaFile;
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
        if (isProductionMode()) {
            return;
        }
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        Location location = findRelevantLocation(component.getClass(), stack,
                null);
        if (isNavigatorCreate(location)) {
            location = findRelevantLocation(null, stack, null);
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
        if (isProductionMode()) {
            return;
        }
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        // In most cases the interesting attach call is found in the same class
        // where the component was created and not in a generic layout class
        Location location = findRelevantLocation(component.getClass(), stack,
                findCreate(component));
        if (isNavigatorCreate(location)) {
            // For routes, we can just show the init location as we have nothing
            // better
            location = createLocation.get(component);
        }
        attachLocation.put(component, location);
    }

    /**
     * Refreshes location of all components that had create or attach location
     * below given reference component by given offset value. Location may
     * change due to dynamic code updates conducted by Vaadin developer tools.
     *
     * @param location
     *            reference component location
     * @param offset
     *            difference in lines to be applied
     */
    public static void refreshLocation(Location location, int offset) {
        refreshLocation(createLocation, location, offset);
        refreshLocation(attachLocation, location, offset);
    }

    private static void refreshLocation(Map<Component, Location> targetRef,
            Location location, int offset) {
        Map<Component, Location> updatedLocations = new HashMap<>();
        targetRef.entrySet().stream().filter(
                e -> Objects.equals(e.getValue().className, location.className))
                .filter(e -> e.getValue().lineNumber > location.lineNumber)
                .forEach(e -> {
                    Location l = e.getValue();
                    updatedLocations.put(e.getKey(), new Location(l.className,
                            l.filename, l.methodName, l.lineNumber + offset));
                });
        targetRef.putAll(updatedLocations);
    }

    private static boolean isNavigatorCreate(Location location) {
        return location.className()
                .equals(AbstractNavigationStateRenderer.class.getName());
    }

    private static Location findRelevantLocation(
            Class<? extends Component> excludeClass, StackTraceElement[] stack,
            Location preferredClass) {
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
                            .equals(preferredClass.className()))
                    .findFirst();
            if (preferredCandidate.isPresent()) {
                return toLocation(preferredCandidate.get());
            }
        }
        return toLocation(candidates.stream().findFirst().orElse(null));
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
