/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.ParameterFormat;
import com.vaadin.flow.router.UrlParameters;
import com.vaadin.flow.server.AmbiguousRouteConfigurationException;
import com.vaadin.flow.server.InvalidRouteConfigurationException;

/**
 * Define a route url segment tree data model which is used to store internally
 * registered routes.
 *
 * A segment may contain a set of the next segment(s) in route(s) and also a
 * {@link RouteTarget} in case this segment is also the last which defines a
 * route.
 */
class RouteModel implements Serializable {

    /**
     * Create a new root segment instance. This is an empty segment defining the
     * root of the routes tree.
     */
    static RouteModel create() {
        return new RouteModel();
    }

    /**
     * Returns whether the specified pathPattern contains url parameters.
     *
     * @param pathPattern
     *            a path pattern.
     * @return true if the specified pathPattern contains url parameters,
     *         otherwise false.
     */
    static boolean hasParameters(String pathPattern) {
        return pathPattern.contains(":");
    }

    /**
     * Define a route url segment tree data model which is used to store
     * internally registered routes.
     * <p>
     * A segment may contain a set of the next segment(s) in route(s) and also a
     * {@link RouteTarget} in case this segment is also the last which defines a
     * route.
     */
    private static class RouteSegment implements Serializable {

        /**
         * Create a new root segment instance. This is an empty segment defining
         * the root of the routes tree.
         */
        static RouteSegment createRoot() {
            return new RouteSegment("");
        }

        private static boolean isParameter(String segmentPattern) {
            return segmentPattern.contains(":");
        }

        private static boolean isVarargsParameter(String segmentPattern) {
            return segmentPattern.startsWith("...:");
        }

        private static boolean isOptionalParameter(String segmentPattern) {
            return segmentPattern.startsWith("[:");
        }

        /**
         * Define a route url parameter details.
         */
        private static class ParameterDetails implements Serializable {

            // NOTE: string may be omited when defining a parameter. If the
            // type/regex is missing then string is used by default.
            private static List<String> PRIMITIVE_TYPES = Arrays.asList("int",
                    "long", "bool", "boolean", "string");

            private boolean optional;

            private boolean varargs;

            // Same name as with the segment's name which is extracted by
            // ParameterDetails constructor and provided to the RouteSegment.
            private String name;

            // Regex or a primitive type.
            private String regex;

            private Pattern pattern;

            /**
             * Creates the parameter details using the input segmentPattern
             * pattern.
             *
             * @param segmentPattern
             *            segment pattern.
             */
            private ParameterDetails(String segmentPattern) {
                optional = segmentPattern.startsWith("[")
                        && segmentPattern.endsWith("]");
                if (optional) {
                    segmentPattern = segmentPattern.substring(1,
                            segmentPattern.length() - 1);
                }

                varargs = segmentPattern.startsWith("...");
                if (varargs) {
                    segmentPattern = segmentPattern.substring(3);
                }

                // Remove :
                segmentPattern = segmentPattern.substring(1);

                // Extract the pattern defining the value of the parameter.
                final int defStartIndex = segmentPattern.indexOf(":");
                if (defStartIndex != -1) {

                    name = segmentPattern.substring(0, defStartIndex);

                    regex = segmentPattern.substring(defStartIndex + 1);
                    if (!isPrimitiveType()) {
                        pattern = Pattern.compile(regex);
                    }

                } else {
                    name = segmentPattern;
                    regex = "string";
                }

            }

            boolean isPrimitiveType() {
                return PRIMITIVE_TYPES.contains(regex);
            }

            public String getType() {
                return regex;
            }

            boolean isOptional() {
                return optional;
            }

            boolean isVarargs() {
                return varargs;
            }

            boolean isMandatory() {
                return !isOptional() && !isVarargs();
            }

            boolean isEligible(String value) {
                if (regex == null) {
                    return true;
                }

                if (pattern != null) {
                    return pattern.matcher(value).matches();
                }

                if (regex.equals("int")) {
                    try {
                        Integer.valueOf(value);
                        return true;
                    } catch (NumberFormatException e) {
                    }

                } else if (regex.equals("long")) {
                    try {
                        Long.valueOf(value);
                        return true;
                    } catch (NumberFormatException e) {
                    }

                } else if (regex.equals("bool") || regex.equals("boolean")) {
                    if (value.equalsIgnoreCase("true")
                            || value.equalsIgnoreCase("false")) {
                        return true;
                    }

                } else if (regex.equals("string")) {
                    return true;
                }

                return false;
            }

        }

        /**
         * Name of the segment.
         */
        private String name;

        /**
         * Segment pattern string as provided in constructor. This is used
         * internally as a key in the parent's mapping, to make clear
         * distinction between static segment values and parameters which are
         * defined as a pattern used to extract the value from a url path.
         */
        private String segmentPattern;

        /**
         * Mapping next segments in the routes by the segment pattern.
         */
        private Map<String, RouteSegment> staticSegments;

        /**
         * Mapping next parameter segments in the routes by the segment pattern.
         */
        private Map<String, RouteSegment> parameterSegments;

        /**
         * Mapping varargs parameter segments in the routes by the segment
         * pattern.
         */
        private Map<String, RouteSegment> varargsSegments;

        /**
         * Track the mapping of all segment types in the routes by the segment
         * pattern.
         */
        private Map<String, RouteSegment> allSegments;

        /**
         * This is valid only if the segment represents a url parameter.
         */
        private ParameterDetails parameterDetails;

        /**
         * Target.
         */
        private RouteTarget target;

        private RouteSegment() {
        }

        private RouteSegment(String segment) {
            segmentPattern = segment;

            if (isParameter(segment)) {
                parameterDetails = new ParameterDetails(segment);

                name = parameterDetails.name;

            } else {
                name = segment;
            }
        }

        /**
         * Collects all routes in an unmodifiable {@link Map}.
         *
         * @return a {@link Map} containing all paths and their specific
         *         targets.
         */
        Map<String, RouteTarget> getRoutes() {

            Map<String, RouteTarget> result = new HashMap<>();

            if (target != null) {
                result.put("", target);
            }

            collectRoutes(result, getStaticSegments());
            collectRoutes(result, getParameterSegments());
            collectRoutes(result, getVarargsSegments());

            if (segmentPattern.isEmpty()) {
                return Collections.unmodifiableMap(result);
            } else {
                return result;
            }
        }

        void removePath(String pathPattern) {
            removePath(PathUtil.getSegmentsList(pathPattern));
        }

        void addPath(String pathPattern,
                Class<? extends Component> targetComponentClass) {
            addPath(pathPattern, new RouteTarget(targetComponentClass));
        }

        /**
         * Add a pathPattern pattern following this route segment. If the
         * pattern already exists and exception is thrown.
         *
         * @param pathPattern
         *            a path pattern where parameters are defined by their ids
         *            and details.
         * @param target
         *            target to set for the given path pattern
         */
        void addPath(String pathPattern, RouteTarget target) {
            addPath(PathUtil.getSegmentsList(pathPattern), target);
        }

        /**
         * Finds a route for the given path.
         *
         * @param path
         *            real navigation path where the parameters are provided
         *            with their real value. The method is looking to map the
         *            value provided in the path with the ids found in the
         *            stored patterns.
         * @return a route result containing the target and parameter values
         *         mapped by their ids.
         */
        RouteSearchResult getRoute(String path) {

            Map<String, Object> urlParameters = new HashMap<>();

            RouteTarget target = findRouteTarget(PathUtil.getSegmentsList(path),
                    urlParameters);

            return new RouteSearchResult(path, target, urlParameters);
        }

        /**
         * Gets a simple representation of the path patter.
         * 
         * @param pathPattern
         *            the full path pattern.
         * @param parameterFormat
         *            the parameter format function.
         * @return the simple path pattern.
         */
        String getPath(String pathPattern,
                Function<RouteSegment, String> parameterFormat) {
            final List<String> segments = PathUtil.getSegmentsList(pathPattern);
            final List<String> result = new ArrayList<>(segments.size());

            matchSegments(segments, routeSegment -> {
                result.add(routeSegment.isParameter()
                        ? parameterFormat.apply(routeSegment)
                        : routeSegment.getName());
            });

            if (result.isEmpty()) {
                return "";
            } else {
                return String.join("/", result);
            }
        }

        String getUrl(String pathPattern, UrlParameters parameters) {
            final List<String> segments = PathUtil.getSegmentsList(pathPattern);
            final List<String> result = new ArrayList<>(segments.size());

            matchSegments(segments, routeSegment -> {
                String segment = routeSegment.getSegmentPattern();

                if (routeSegment.isParameter()) {

                    final String parameterName = routeSegment.getName();

                    if (routeSegment.getParameterDetails().isVarargs()) {
                        final List<String> args = parameters
                                .getList(parameterName);

                        if (args != null) {
                            for (String value : args) {
                                if (!routeSegment.getParameterDetails()
                                        .isEligible(value)) {
                                    throw new IllegalArgumentException(
                                            "Url varargs parameter `"
                                                    + parameterName
                                                    + "` has a specified value `"
                                                    + value
                                                    + "`, which is invalid according to the parameter definition `"
                                                    + segment + "`");
                                }

                                result.add(value);
                            }
                        }

                        // Varargs are always last so no need to even try going
                        // forward.
                        return;

                    } else {
                        final String value = parameters.get(parameterName);

                        if (value == null && routeSegment.getParameterDetails()
                                .isMandatory()) {
                            throw new IllegalArgumentException("Url parameter `"
                                    + parameterName
                                    + "` is mandatory but missing from the parameters argument.");
                        }

                        if (value != null && !routeSegment.getParameterDetails()
                                .isEligible(value)) {
                            throw new IllegalArgumentException("Url parameter `"
                                    + parameterName + "` has specified value `"
                                    + value
                                    + "`, which is invalid according to the parameter definition `"
                                    + segment + "`");
                        }

                        if (value != null) {
                            result.add(value);
                        }
                    }

                } else {
                    result.add(segment);
                }
            });

            if (result.isEmpty()) {
                return "";
            } else {
                return String.join("/", result);
            }
        }

        private String getName() {
            return name;
        }

        private String getSegmentPattern() {
            return segmentPattern;
        }

        private boolean isParameter() {
            return parameterDetails != null;
        }

        private ParameterDetails getParameterDetails() {
            return parameterDetails;
        }

        private boolean hasTarget() {
            return target != null;
        }

        private RouteTarget getTarget() {
            return target;
        }

        private void collectRoutes(Map<String, RouteTarget> result,
                Map<String, RouteSegment> children) {
            for (Map.Entry<String, RouteSegment> segmentEntry : children
                    .entrySet()) {

                for (Map.Entry<String, RouteTarget> targetEntry : segmentEntry
                        .getValue().getRoutes().entrySet()) {

                    final String key = targetEntry.getKey();
                    result.put(
                            segmentEntry.getKey()
                                    + (key.isEmpty() ? "" : ("/" + key)),
                            targetEntry.getValue());
                }
            }
        }

        private void removePath(List<String> segmentPatterns) {
            RouteSegment routeSegment;
            String segmentPattern = null;
            Map<String, RouteSegment> children = null;

            if (segmentPatterns.isEmpty()) {
                // This should happen only on root.
                routeSegment = this;

            } else {
                segmentPattern = segmentPatterns.get(0);

                children = getChildren(segmentPattern);
                routeSegment = children.get(segmentPattern);
            }

            if (routeSegment != null) {

                if (segmentPatterns.size() > 1) {
                    routeSegment.removePath(
                            segmentPatterns.subList(1, segmentPatterns.size()));
                } else {
                    routeSegment.target = null;
                }

                if (routeSegment.isEmpty() && routeSegment != this) {
                    removeSegment(segmentPattern, children);
                }
            }
        }

        private void addPath(List<String> segmentPatterns, RouteTarget target) {

            RouteSegment routeSegment;
            String segmentPattern = null;
            Map<String, RouteSegment> children = null;

            if (segmentPatterns.isEmpty()) {
                // This should happen only on root.
                routeSegment = this;

            } else {
                segmentPattern = segmentPatterns.get(0);

                children = getChildren(segmentPattern);
                routeSegment = children.get(segmentPattern);
            }

            if (routeSegment == null) {

                // We reject any route where varargs is not the last segment.
                if (isVarargsParameter(segmentPattern)
                        && segmentPatterns.size() > 1) {
                    throw new IllegalArgumentException(
                            "A varargs url parameter may be defined only as the last path segment");
                }

                // We reject any route where the last segment is an optional
                // parameter while there's already a target set for the same
                // route without the optional parameter.
                if (isOptionalParameter(segmentPattern)
                        && segmentPatterns.size() == 1 && hasTarget()) {
                    throw ambigousOptionalTarget(target.getTarget(),
                            getTarget().getTarget());
                }

                routeSegment = addSegment(segmentPattern, children);
            }

            addPath(routeSegment, segmentPatterns, target);
        }

        private void addPath(RouteSegment potentialSegment,
                List<String> segmentPatterns, RouteTarget target) {
            if (segmentPatterns.size() > 1) {
                potentialSegment.addPath(
                        segmentPatterns.subList(1, segmentPatterns.size()),
                        target);

            } else {
                if (!potentialSegment.hasTarget()) {

                    // We reject any route where there's already a target set
                    // for the same route with an optional.
                    RouteSegment optional = potentialSegment
                            .getOptionalParameterWithTarget();
                    if (optional != null) {
                        throw optional.ambigousOptionalTarget(
                                optional.getTarget().getTarget(),
                                target.getTarget());
                    }

                    potentialSegment.target = target;

                } else {
                    throw potentialSegment.ambigousTarget(target.getTarget());
                }
            }
        }

        private RouteTarget findRouteTarget(List<String> segments,
                Map<String, Object> urlParameters) {

            // First try with a static segment (non a parameter). An empty
            // segments list should happen only on root, so this instance should
            // resemble only the root.
            RouteSegment routeSegment = segments.isEmpty() ? this
                    : getStaticSegments().get(segments.get(0));

            if (routeSegment != null) {
                RouteTarget target = findRouteTarget(routeSegment, segments,
                        urlParameters);
                if (target != null) {
                    return target;
                }
            }

            // If no route following a static segment was found try through
            // parameters.
            if (!segments.isEmpty()) {

                for (RouteSegment parameter : getParameterSegments().values()) {
                    RouteTarget target = findRouteTarget(parameter, segments,
                            urlParameters);
                    if (target != null) {
                        return target;
                    }
                }

                for (RouteSegment parameter : getParameterSegments().values()) {
                    // Try ignoring the parameter if optional and look into its
                    // children using the same segments.
                    if (parameter.getParameterDetails().isOptional()) {
                        Map<String, Object> outputParameters = new HashMap<>();
                        target = parameter.findRouteTarget(segments,
                                outputParameters);

                        if (target != null) {
                            urlParameters.putAll(outputParameters);
                            return target;
                        }
                    }
                }

                for (RouteSegment varargParameter : getVarargsSegments()
                        .values()) {
                    RouteTarget target = findRouteTarget(varargParameter,
                            segments, urlParameters);
                    if (target != null) {
                        return target;
                    }
                }
            }

            return null;
        }

        private RouteTarget findRouteTarget(RouteSegment potentialSegment,
                List<String> segments, Map<String, Object> urlParameters) {

            Map<String, Object> outputParameters = new HashMap<>();

            if (potentialSegment.isParameter()) {

                // Handle varargs.
                if (potentialSegment.getParameterDetails().isVarargs()) {

                    for (String value : segments) {
                        if (!potentialSegment.getParameterDetails()
                                .isEligible(value)) {
                            // If any value is not eligible we don't want to go
                            // any further.
                            return null;
                        }
                    }

                    outputParameters.put(potentialSegment.getName(),
                            Collections.unmodifiableList(segments));
                    segments = Collections.emptyList();

                } else {
                    // Handle one parameter value.
                    String value = segments.get(0);

                    if (potentialSegment.getParameterDetails()
                            .isEligible(value)) {
                        outputParameters.put(potentialSegment.getName(), value);

                    } else {
                        // If the value is not eligible we don't want to go any
                        // further.
                        return null;
                    }
                }
            }

            RouteTarget target;

            segments = segments.size() <= 1 ? Collections.emptyList()
                    : segments.subList(1, segments.size());

            if (segments.size() > 0) {
                // Continue looking if there any more segments.
                target = potentialSegment.findRouteTarget(segments,
                        outputParameters);

            } else if (potentialSegment.hasTarget()) {
                // Found target.
                target = potentialSegment.getTarget();

            } else {
                // Look for target in optional children.
                RouteSegment optionalChild = potentialSegment
                        .getAnyOptionalOrVarargsParameterWithTarget();
                if (optionalChild != null) {
                    target = optionalChild.getTarget();
                } else {
                    target = null;
                }
            }

            if (target != null) {
                urlParameters.putAll(outputParameters);
            }

            return target;
        }

        private void matchSegments(List<String> segments,
                Consumer<RouteSegment> segmentProcessor) {
            if (segments.isEmpty()) {
                return;
            }

            RouteSegment routeSegment = getAllSegments().get(segments.get(0));

            if (routeSegment == null) {
                throw new IllegalArgumentException(
                        "Unregistered path pattern specified `"
                                + PathUtil.getPath(segments) + "`");
            }

            segmentProcessor.accept(routeSegment);

            if (segments.size() > 1) {
                routeSegment.matchSegments(segments.subList(1, segments.size()),
                        segmentProcessor);
            }
        }

        /**
         * Returns any optional or varargs (since that's optional too) parameter
         * child with a target set so in case there's no target on a potential
         * targeted segment we use the target from the optional child. The
         * search is performed recursively on this segment.
         */
        private RouteSegment getAnyOptionalOrVarargsParameterWithTarget() {
            RouteSegment optionalParameter = getOptionalParameterWithTarget();
            if (optionalParameter != null) {
                return optionalParameter;
            }

            // Try looking into children.
            for (RouteSegment parameter : getParameterSegments().values()) {
                if (parameter.getParameterDetails().isOptional()) {
                    return parameter
                            .getAnyOptionalOrVarargsParameterWithTarget();
                }
            }

            // Move to varargs.
            final Map<String, RouteSegment> varargsSegments = getVarargsSegments();
            if (!varargsSegments.isEmpty()) {
                return varargsSegments.values().iterator().next();

            } else {
                return null;
            }
        }

        /**
         * Returns a child optional parameter with target.
         */
        private RouteSegment getOptionalParameterWithTarget() {
            for (RouteSegment parameter : getParameterSegments().values()) {
                if (parameter.getParameterDetails().isOptional()
                        && parameter.hasTarget()) {
                    return parameter;
                }
            }
            return null;
        }

        private RuntimeException ambigousOptionalTarget(
                Class<? extends Component> optionalTarget,
                Class<? extends Component> otherTarget) {
            String message = String.format(
                    "Navigation targets '%s' and '%s' have the same path and '%s' has an OptionalParameter that will never be used as optional.",
                    otherTarget.getName(), optionalTarget.getName(),
                    optionalTarget.getName());
            throw ambigousException(message);
        }

        private RuntimeException ambigousTarget(
                Class<? extends Component> target) {

            String messageFormat;
            if (isParameter()) {
                messageFormat = "Navigation targets must have unique routes, found navigation targets '%s' and '%s' with parameter have the same route.";
            } else {
                messageFormat = "Navigation targets must have unique routes, found navigation targets '%s' and '%s' with the same route.";
            }

            String message = String.format(messageFormat,
                    getTarget().getTarget().getName(), target.getName());
            throw ambigousException(message);
        }

        private RuntimeException ambigousException(String message) {
            throw new AmbiguousRouteConfigurationException(message,
                    getTarget().getTarget());
        }

        private boolean isEmpty() {
            return target == null && getStaticSegments().isEmpty()
                    && getParameterSegments().isEmpty()
                    && getVarargsSegments().isEmpty();
        }

        private RouteSegment addSegment(String segmentPattern,
                Map<String, RouteSegment> children) {
            RouteSegment routeSegment = new RouteSegment(segmentPattern);
            children.put(segmentPattern, routeSegment);
            getAllSegments().put(segmentPattern, routeSegment);
            return routeSegment;
        }

        private void removeSegment(String segmentPattern,
                Map<String, RouteSegment> children) {
            children.remove(segmentPattern);
            getAllSegments().remove(segmentPattern);
        }

        /**
         * Gets the children mapping, either static segments or parameters,
         * which are siblings to segmentPattern.
         */
        private Map<String, RouteSegment> getChildren(String segmentPattern) {
            return isVarargsParameter(segmentPattern) ? getVarargsSegments()
                    : isParameter(segmentPattern) ? getParameterSegments()
                            : getStaticSegments();
        }

        private Map<String, RouteSegment> getStaticSegments() {
            if (staticSegments == null) {
                staticSegments = new HashMap<>();
            }
            return staticSegments;
        }

        private Map<String, RouteSegment> getParameterSegments() {
            if (parameterSegments == null) {
                // Parameters iteration must be based on insertion.
                parameterSegments = new LinkedHashMap<>();
            }
            return parameterSegments;
        }

        private Map<String, RouteSegment> getVarargsSegments() {
            if (varargsSegments == null) {
                // Parameters iteration must be based on insertion.
                varargsSegments = new LinkedHashMap<>();
            }
            return varargsSegments;
        }

        private Map<String, RouteSegment> getAllSegments() {
            if (allSegments == null) {
                allSegments = new HashMap<>();
            }
            return allSegments;
        }

    }

    private RouteSegment root;

    private RouteModel() {
        root = RouteSegment.createRoot();
    }

    /**
     * Collects all routes in an unmodifiable {@link Map}.
     *
     * @return a {@link Map} containing all paths and their specific targets.
     */
    Map<String, RouteTarget> getRoutes() {
        return root.getRoutes();
    }

    /**
     * Remove a path by it's path pattern.
     * 
     * @param pathPattern
     *            the full path pattern.
     */
    void removePath(String pathPattern) {
        root.removePath(pathPattern);
    }

    /**
     * Add a path pattern which maps a target component class.
     * 
     * @param pathPattern
     *            a path pattern where parameters are defined by their ids and
     *            details.
     * @param targetComponentClass
     *            the target component class.
     * @throws InvalidRouteConfigurationException
     *             if the combination of pathPattern and target doesn't make
     *             send within the current state of the model.
     * @throws IllegalArgumentException
     *             in case the varargs are specified in the middle of the
     *             pathPattern. Varargs may be specified only as the last
     *             segment definition.
     */
    void addRoute(String pathPattern,
            Class<? extends Component> targetComponentClass) {
        root.addPath(pathPattern, targetComponentClass);
    }

    /**
     * Add a pathPattern pattern following this route segment. If the pattern
     * already exists and exception is thrown.
     *
     * @param pathPattern
     *            a path pattern where parameters are defined by their ids and
     *            details.
     * @param target
     *            target to set for the given path pattern.
     * @throws InvalidRouteConfigurationException
     *             if the combination of pathPattern and target doesn't make
     *             send within the current state of the model.
     * @throws IllegalArgumentException
     *             in case the varargs are specified in the middle of the
     *             pathPattern. Varargs may be specified only as the last
     *             segment definition.
     */
    void addRoute(String pathPattern, RouteTarget target) {
        root.addPath(pathPattern, target);
    }

    /**
     * Finds a route for the given path.
     *
     * @param path
     *            real navigation path where the parameters are provided with
     *            their real value. The method is looking to map the value
     *            provided in the path with the ids found in the stored
     *            patterns.
     * @return a route result containing the target and parameter values mapped
     *         by their ids.
     */
    RouteSearchResult getRoute(String path) {
        return root.getRoute(path);
    }

    /**
     * Gets a url path by replacing into the path pattern the url parameters.
     * <p>
     * In case all parameters defined in the pathPattern are optional or
     * varargs, parameters argument may be null and the path will be provided
     * without any parameters.
     * 
     * @param pathPattern
     *            the full path pattern.
     * @param parameters
     *            the parameters to use or null if no parameters specified.
     * @return the url.
     * @throws IllegalArgumentException
     *             in case pathPattern is not registered or the parameters do
     *             not match with the pattern.
     */
    String getUrl(String pathPattern, UrlParameters parameters) {
        return root.getUrl(pathPattern,
                parameters != null ? parameters : new UrlParameters(null));
    }

    String getRoute(String pathPattern, EnumSet<ParameterFormat> format) {
        return root.getPath(pathPattern, segment -> {
            StringBuilder result = new StringBuilder();

            if (format.contains(ParameterFormat.CURLY_BRACKETS_FORMAT)) {
                result.append("{");
            } else {
                result.append(":");
            }

            final boolean containsType = format.containsAll(Arrays
                    .asList(ParameterFormat.SIMPLE_TYPE, ParameterFormat.TYPE));

            if (format.contains(ParameterFormat.NAME)) {
                result.append(segment.getName());
                if (containsType) {
                    result.append(":");
                }
            }

            if (containsType) {
                String type = segment.getParameterDetails().getType();

                if (format.contains(ParameterFormat.SIMPLE_TYPE)) {
                    if (!segment.getParameterDetails().isPrimitiveType()) {
                        type = "regex";
                    }

                    if (format.contains(ParameterFormat.CAPITALIZED_TYPE)) {
                        type = capitalize(type);
                    }

                } else if (segment.getParameterDetails().isPrimitiveType()
                        && format.contains(ParameterFormat.CAPITALIZED_TYPE)) {
                    type = capitalize(type);
                }

                result.append(type);
            }

            if (result.charAt(0) == '{') {
                result.append("}");
            }

            return result.toString();
        });
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

}
