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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vaadin.flow.component.Component;

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
                    if (!PRIMITIVE_TYPES.contains(regex)) {
                        pattern = Pattern.compile(regex);
                    }

                } else {
                    name = segmentPattern;
                }

            }

            boolean isOptional() {
                return optional;
            }

            boolean isVarargs() {
                return varargs;
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
            // In case of a parameter this is stored as the entire input value.
            segmentPattern = segment.trim();

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

        private Optional<RouteTarget> getTarget() {
            return Optional.ofNullable(target);
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

        void removePath(List<String> segmentPatterns) {
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

                if (routeSegment.isEmpty()) {
                    children.remove(segmentPattern);
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
                    throw new IllegalArgumentException(
                            "Same route without optional is already set. Please make the last optional parameter mandatory.");
                }

                routeSegment = new RouteSegment(segmentPattern);
                children.put(routeSegment.segmentPattern, routeSegment);

            } else {
                // We reject any route where there's already a target set for
                // the same route with an optional.
                if (segmentPatterns.size() == 1
                        && getOptionalParameterWithTarget() != null) {
                    throw new IllegalArgumentException(
                            "Same route with optional is set. Please change the optional from the other route to be mandatory.");
                }
            }

            if (segmentPatterns.size() > 1) {
                routeSegment.addPath(
                        segmentPatterns.subList(1, segmentPatterns.size()),
                        target);

            } else {
                if (!routeSegment.hasTarget()) {
                    routeSegment.target = target;
                } else {
                    throw new IllegalArgumentException(
                            "Target already configured for specified path");
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
                List<String> segments,
                Map<String, Object> urlParameters) {

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

            } else if (potentialSegment.target != null) {
                // Found target.
                target = potentialSegment.target;

            } else {
                // Look for target in optional children.
                RouteSegment optionalChild = potentialSegment
                        .getAnyOptionalOrVarargsParameterWithTarget();
                if (optionalChild != null) {
                    target = optionalChild.target;
                } else {
                    target = null;
                }
            }

            if (target != null) {
                urlParameters.putAll(outputParameters);
            }

            return target;
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

        private boolean isEmpty() {
            return target == null && getStaticSegments().isEmpty()
                    && getParameterSegments().isEmpty()
                    && getVarargsSegments().isEmpty();
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

    void removePath(String pathPattern) {
        root.removePath(pathPattern);
    }

    void addPath(String pathPattern,
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
     *            target to set for the given path pattern
     */
    void addPath(String pathPattern, RouteTarget target) {
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

}
