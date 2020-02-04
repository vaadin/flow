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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
     * Define a route url segment tree data model which is used to store internally
     * registered routes.
     * <p>
     * A segment may contain a set of the next segment(s) in route(s) and also a
     * {@link RouteTarget} in case this segment is also the last which defines a
     * route.
     */
    private static class RouteSegment implements Serializable {

        /**
         * Create a new root segment instance. This is an empty segment defining the
         * root of the routes tree.
         */
        static RouteSegment createRoot() {
            return new RouteSegment("");
        }

        /**
         * Returns whether the specified pathPattern contains url parameters.
         *
         * @param pathPattern a path pattern.
         * @return true if the specified pathPattern contains url parameters,
         * otherwise false.
         */
        static boolean hasParameters(String pathPattern) {
            return pathPattern.contains(":");
        }

        private static boolean isParameter(String segmentPattern) {
            return segmentPattern.contains(":");
        }

        private static boolean isVarargsParameter(String segmentPattern) {
            return segmentPattern.startsWith("...:")
                    // In case of optional parameter.
                    || segmentPattern.startsWith("[...:");
        }

        private static boolean isOptionalParameter(String segmentPattern) {
            return segmentPattern.startsWith("[:")
                    || segmentPattern.startsWith("[...:");
        }

        /**
         * Define a route url parameter details.
         */
        private class ParameterDetails implements Serializable {

            private boolean optional;

            private boolean varargs;

            // Same name as with the segment's name which is extracted by
            // ParameterDetails constructor and provided to the RouteSegment.
            private String name;

            // In case the eligible type is a primitive this is set to the type of
            // the primitive.
            private String eligiblePrimitiveType;

            /**
             * Creates the parameter details using the input segmentPattern pattern.
             *
             * @param segmentPattern segment pattern.
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

                    String typeDefPattern = segmentPattern
                            .substring(defStartIndex + 1);

                    extractTypeDef(typeDefPattern);

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

                Optional<Boolean> primitiveEligible = isPrimitiveEligible(value);

                if (primitiveEligible.isPresent()) {
                    return primitiveEligible.get();
                }

                return true;
            }

            private Optional<Boolean> isPrimitiveEligible(String value) {
                if (eligiblePrimitiveType != null) {

                    if (eligiblePrimitiveType.equals("int")) {

                        try {
                            Integer.valueOf(value);

                            return Optional.of(Boolean.TRUE);
                        } catch (NumberFormatException e) {
                        }
                    } else if (eligiblePrimitiveType.equals("long")) {

                        try {
                            Long.valueOf(value);

                            return Optional.of(Boolean.TRUE);
                        } catch (NumberFormatException e) {
                        }

                    } else if (eligiblePrimitiveType.equals("bool")
                            || eligiblePrimitiveType.equals("boolean")) {
                        if (value.equalsIgnoreCase("true")
                                || value.equalsIgnoreCase("false")) {
                            return Optional.of(Boolean.TRUE);
                        }
                    }

                    return Optional.of(Boolean.FALSE);
                }

                return Optional.empty();
            }

            private void extractTypeDef(String patternDef) {
                // This implementation may be updated for more type options,
                // including regex eventually.
                extractPrimitiveTypeDef(patternDef);
            }

            private void extractPrimitiveTypeDef(String patternDef) {
                // We expect only primitives for now.
                eligiblePrimitiveType = patternDef;
            }

        }

        /**
         * Name of the segment.
         */
        private String name;

        /**
         * Segment pattern string as provided in constructor. This is used
         * internally as a key in the parent's mapping, to make clear distinction
         * between static segment values and parameters which are defined as a
         * pattern used to extract the value from a url path.
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
         * Mapping varargs parameter segments in the routes by the segment pattern.
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
         * @return a {@link Map} containing all paths and their specific targets.
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
         * Add a pathPattern pattern following this route segment. If the pattern
         * already exists and exception is thrown.
         *
         * @param pathPattern a path pattern where parameters are defined by their ids and
         *                    details.
         * @param target      target to set for the given path pattern
         */
        void addPath(String pathPattern, RouteTarget target) {
            addPath(PathUtil.getSegmentsList(pathPattern), target);
        }

        /**
         * Finds a route for the given path.
         *
         * @param path real navigation path where the parameters are provided with
         *             their real value. The method is looking to map the value
         *             provided in the path with the ids found in the stored
         *             patterns.
         * @return a route result containing the target and parameter values mapped
         * by their ids.
         */
        RouteSearchResult getRoute(String path) {

            Map<String, Serializable> urlParameters = new HashMap<>();

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

                if (isVarargsParameter(segmentPattern)
                        && segmentPatterns.size() > 1) {
                    throw new IllegalArgumentException(
                            "A varargs url parameter may be defined only as the last path segment");
                }

                if (isParameter(segmentPattern) && segmentPatterns.size() == 1) {

                    // TODO implement exceptions

                }

                routeSegment = new RouteSegment(segmentPattern);
                children.put(routeSegment.segmentPattern, routeSegment);
            }

            if (segmentPatterns.size() > 1) {
                routeSegment.addPath(
                        segmentPatterns.subList(1, segmentPatterns.size()), target);

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
                                            Map<String, Serializable> urlParameters) {

            // First try with a static segment (non a parameter). An empty segments
            // list should happen only on root, so this instance should resemble
            // only the root.
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
                        Map<String, Serializable> outputParameters = new HashMap<>();
                        target = parameter.findRouteTarget(segments,
                                outputParameters);

                        if (target != null) {
                            urlParameters.putAll(outputParameters);
                            return target;
                        }
                    }
                }

                for (RouteSegment varargParameter : getVarargsSegments().values()) {
                    RouteTarget target = findRouteTarget(varargParameter, segments,
                            urlParameters);
                    if (target != null) {
                        return target;
                    }
                }
            }

            return null;
        }

        private RouteTarget findRouteTarget(RouteSegment potentialSegment,
                                            List<String> segments, Map<String, Serializable> urlParameters) {

            Map<String, Serializable> outputParameters = new HashMap<>();

            if (potentialSegment.isParameter()) {

                // Handle varargs.
                if (potentialSegment.getParameterDetails().isVarargs()) {

                    for (String value : segments) {
                        if (!potentialSegment.getParameterDetails()
                                .isEligible(value)) {
                            // If any value is not eligible we don't want to go any
                            // further.
                            return null;
                        }
                    }

                    outputParameters.put(potentialSegment.getName(),
                            new ArrayList<>(segments));
                    segments = Collections.emptyList();

                } else {
                    // Handle one parameter value.
                    String value = segments.get(0);

                    if (potentialSegment.getParameterDetails().isEligible(value)) {
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
                target = potentialSegment.findRouteTarget(segments,
                        outputParameters);
            } else {
                target = potentialSegment.target;
            }

            if (target != null) {
                urlParameters.putAll(outputParameters);
            }

            return target;
        }

        private boolean isEmpty() {
            return target == null && getStaticSegments().isEmpty()
                    && getParameterSegments().isEmpty()
                    && getVarargsSegments().isEmpty();
        }

        /**
         * Gets the children mapping, either static segments or parameters, which
         * are siblings to segmentPattern.
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
                parameterSegments = new HashMap<>();
            }
            return parameterSegments;
        }

        private Map<String, RouteSegment> getVarargsSegments() {
            if (varargsSegments == null) {
                varargsSegments = new HashMap<>();
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
     * @param pathPattern a path pattern where parameters are defined by their ids and
     *                    details.
     * @param target      target to set for the given path pattern
     */
    void addPath(String pathPattern, RouteTarget target) {
        root.addPath(pathPattern, target);
    }

    /**
     * Finds a route for the given path.
     *
     * @param path real navigation path where the parameters are provided with
     *             their real value. The method is looking to map the value
     *             provided in the path with the ids found in the stored
     *             patterns.
     * @return a route result containing the target and parameter values mapped
     * by their ids.
     */
    RouteSearchResult getRoute(String path) {
        return root.getRoute(path);
    }

}