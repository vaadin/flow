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
package com.vaadin.flow.component.internal;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.internal.AbstractNavigationStateRenderer;
import com.vaadin.flow.server.AbstractConfiguration;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

/**
 * Tracks the location in source code where components were instantiated.
 *
 **/
public class ComponentTracker {

    private static Map<Component, Throwable> createThrowable = Collections
            .synchronizedMap(new WeakHashMap<>());
    private static Map<Component, Throwable> attachThrowable = Collections
            .synchronizedMap(new WeakHashMap<>());
    private static Map<Component, Location> createLocation = Collections
            .synchronizedMap(new WeakHashMap<>());
    private static Map<Component, Location> attachLocation = Collections
            .synchronizedMap(new WeakHashMap<>());
    private static Map<Component, Location[]> createLocations = Collections
            .synchronizedMap(new WeakHashMap<>());
    private static Map<Component, Location[]> attachLocations = Collections
            .synchronizedMap(new WeakHashMap<>());
    private static AtomicLong creationOrdinal = new AtomicLong(0);
    private static AtomicLong attachOrdinal = new AtomicLong(0);

    private static Boolean disabled = null;
    private static String[] prefixesToSkip = new String[] {
            "com.vaadin.flow.component.", "com.vaadin.flow.di.",
            "com.vaadin.flow.dom.", "com.vaadin.flow.internal.",
            "com.vaadin.flow.spring.", "com.vaadin.cdi.", "java.", "jdk.",
            "org.springframework.beans.", "org.jboss.weld.", };

    /**
     * Represents a location in the source code.
     */
    public static class Location implements Serializable {
        private static final Pattern MAYBE_INNER_CLASS = Pattern
                .compile("(.*\\.[^$.]+)\\$[^.]+$");
        private final String className;
        private final String filename;
        private final String methodName;
        private final int lineNumber;
        private long ordinal;

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

        public long ordinal() {
            return ordinal;
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
         * Finds the source file this location refers to.
         *
         * @param configuration
         *            the application configuration
         * @return the source file the location refers to, or {@code null}
         */
        public File findSourceFile(AbstractConfiguration configuration) {
            String cls = className();
            int indexOfExt = filename().lastIndexOf(".");
            String ext = filename().substring(indexOfExt);
            if (!ext.equals(".java") && !ext.equals(".kt")) {
                return null;
            }

            String filenameNoExt = filename().substring(0, indexOfExt);

            if (!cls.endsWith(filenameNoExt)) {
                // Check for inner class
                Matcher matcher = MAYBE_INNER_CLASS.matcher(cls);
                if (matcher.find()) {
                    cls = matcher.group(1);
                }
            }
            if (!cls.endsWith(filenameNoExt)) {
                return null;
            }

            File src = configuration.getJavaSourceFolder();

            // Windows path is with '\' and not '/'. normalize path for check.
            String path = src.getPath().replaceAll("\\\\", "/");
            if (ext.equals(".kt") && path.endsWith("/java")) {
                src = new File(path.substring(0, path.lastIndexOf("/java"))
                        + "/kotlin");
            }
            File javaFile = new File(src,
                    cls.replace(".", File.separator) + ext);
            return javaFile;
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
        return computeFilteredLocation(component, createLocation,
                createThrowable, null);
    }

    /**
     * Finds the locations related to where the given component instance was
     * created.
     *
     * @param component
     *            the component to find
     * @return the locations involved in creating the component
     */
    public static Location[] findCreateLocations(Component component) {
        return computeAllLocations(component, createLocations, createThrowable, creationOrdinal);
    }

    /**
     * Tracks the location where the component was created. This should be
     * called from the Component constructor so that the creation location can
     * be found from the current stacktrace.
     * <p>
     * Uses lazy evaluation: only stores a lightweight Throwable. Stack trace
     * processing is deferred until findCreate() or findCreateLocations() is
     * called.
     *
     * @param component
     *            the component to track
     */
    public static void trackCreate(Component component) {
        if (isDisabled()) {
            return;
        }
        createThrowable.put(component, new Throwable());
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
        return computeFilteredLocation(component, attachLocation,
                attachThrowable, findCreate(component));
    }

    /**
     * Finds the locations related to where the given component instance was
     * attached to a parent.
     *
     * @param component
     *            the component to find
     * @return the locations involved in creating the component
     */
    public static Location[] findAttachLocations(Component component) {
        return computeAllLocations(component, attachLocations, attachThrowable, attachOrdinal);
    }

    /**
     * Tracks the location where the component was attached. This should be
     * called from the Component attach logic so that the creation location can
     * be found from the current stacktrace.
     * <p>
     * Uses lazy evaluation: only stores a lightweight Throwable. Stack trace
     * processing is deferred until findAttach() or findAttachLocations() is
     * called.
     *
     * @param component
     *            the component to track
     */
    public static void trackAttach(Component component) {
        if (isDisabled()) {
            return;
        }
        attachThrowable.put(component, new Throwable());
    }

    /**
     * Refreshes location of all components that had create or attach location
     * below given reference component by given offset value. Location may
     * change due to dynamic code updates conducted by Vaadin developer tools.
     * <p>
     * Note: With lazy evaluation, this method forces evaluation of all tracked
     * components to ensure their locations are cached before applying offsets.
     *
     * @param location
     *            reference component location
     * @param offset
     *            difference in lines to be applied
     */
    public static void refreshLocation(Location location, int offset) {
        // Force lazy evaluation for all components before refreshing
        forceLazyEvaluation();

        refreshLocationMap(createLocation, location, offset);
        refreshLocations(createLocations, location, offset);
        refreshLocationMap(attachLocation, location, offset);
        refreshLocations(attachLocations, location, offset);
    }

    private static void forceLazyEvaluation() {
        // Force evaluation of all create locations
        for (Component component : createThrowable.keySet()) {
            findCreate(component);
            findCreateLocations(component);
        }
        // Force evaluation of all attach locations
        for (Component component : attachThrowable.keySet()) {
            findAttach(component);
            findAttachLocations(component);
        }
    }

    private static boolean needsUpdate(Location l, Location referenceLocation) {
        return Objects.equals(l.className, referenceLocation.className)
                && l.lineNumber > referenceLocation.lineNumber;
    }

    private static Location updateLocation(Location l, int offset) {
        return new Location(l.className, l.filename, l.methodName,
                l.lineNumber + offset);
    }

    private static void refreshLocationMap(Map<Component, Location> targetRef,
            Location referenceLocation, int offset) {
        Map<Component, Location> updatedLocations = new HashMap<>();
        for (Component c : targetRef.keySet()) {
            Location l = targetRef.get(c);
            if (needsUpdate(l, referenceLocation)) {
                updatedLocations.put(c, updateLocation(l, offset));
            }
        }

        targetRef.putAll(updatedLocations);
    }

    private static void refreshLocations(Map<Component, Location[]> targetRef,
            Location referenceLocation, int offset) {
        Map<Component, Location[]> updatedLocations = new HashMap<>();
        for (Component c : targetRef.keySet()) {
            Location[] locations = targetRef.get(c);

            for (int i = 0; i < locations.length; i++) {
                if (needsUpdate(locations[i], referenceLocation)) {
                    locations[i] = updateLocation(locations[i], offset);
                }
            }
        }

        targetRef.putAll(updatedLocations);
    }

    private static boolean isNavigatorCreate(Location location) {
        return location.className()
                .equals(AbstractNavigationStateRenderer.class.getName());
    }

    private static Location[] findRelevantLocations(Location[] locations) {
        return Stream.of(locations).filter(location -> {
            for (String prefixToSkip : prefixesToSkip) {
                if (location.className().startsWith(prefixToSkip)) {
                    return false;
                }
            }
            return true;
        }).toArray(Location[]::new);
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

    /**
     * Checks if the component tracking is disabled.
     *
     * Tracking is disabled when application is running in production mode or if
     * the configuration property
     * {@literal vaadin.devmode.componentTracker.enabled} is set to
     * {@literal false}.
     *
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
        ApplicationConfiguration applicationConfiguration = ApplicationConfiguration
                .get(context);
        if (applicationConfiguration == null) {
            return true;
        }

        disabled = applicationConfiguration.isProductionMode()
                || !applicationConfiguration.getBooleanProperty(
                        InitParameters.APPLICATION_PARAMETER_DEVMODE_ENABLE_COMPONENT_TRACKER,
                        true);
        return disabled;
    }

    /**
     * Computes and caches a filtered location (single best location) for a
     * component.
     *
     * @param component
     *            the component
     * @param locationCache
     *            the cache to use
     * @param throwableMap
     *            the map containing Throwables
     * @param referenceLocation
     *            reference location for filtering (or null)
     * @return the computed location or null
     */
    private static Location computeFilteredLocation(Component component,
            Map<Component, Location> locationCache,
            Map<Component, Throwable> throwableMap,
            Location referenceLocation) {
        // Check cache first
        Location cached = locationCache.get(component);
        if (cached != null) {
            return cached;
        }

        // Compute lazily from Throwable on first access
        Throwable throwable = throwableMap.get(component);
        if (throwable == null) {
            return null;
        }

        StackTraceElement[] stack = throwable.getStackTrace();
        Location[] allLocations = Stream.of(stack)
                .map(ComponentTracker::toLocation).toArray(Location[]::new);
        Location[] relevantLocations = findRelevantLocations(allLocations);
        Location location = findRelevantLocation(component.getClass(),
                relevantLocations, referenceLocation);
        if (isNavigatorCreate(location)) {
            location = referenceLocation != null ? referenceLocation
                    : findRelevantLocation(null, relevantLocations, null);
        }

        locationCache.put(component, location);
        return location;
    }

    /**
     * Computes and caches all stack trace locations for a component.
     *
     * @param component
     *            the component
     * @param locationsCache
     *            the cache to use
     * @param throwableMap
     *            the map containing Throwables
     * @return array of all locations or null
     */
    private static Location[] computeAllLocations(Component component,
            Map<Component, Location[]> locationsCache,
            Map<Component, Throwable> throwableMap,
            AtomicLong referenceCounter) {
        // Check cache first
        Location[] cached = locationsCache.get(component);
        if (cached != null) {
            return cached;
        }

        // Convert lazily from Throwable on first access
        Throwable throwable = throwableMap.get(component);
        if (throwable == null) {
            return null;
        }
        Location[] locations = Stream.of(throwable.getStackTrace())
                .map(ComponentTracker::toLocation).toArray(Location[]::new);
        for (Location location : locations) {
            location.ordinal = referenceCounter.getAndIncrement();
        }
        locationsCache.put(component, locations);
        return locations;
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
