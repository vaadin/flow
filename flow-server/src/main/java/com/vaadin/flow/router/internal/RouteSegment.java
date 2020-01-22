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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.vaadin.flow.component.Component;

/**
 * Define a route url segment tree data model which is used to store internally
 * registered routes.
 * 
 * A segment may contain a set of the next segment(s) in route(s) and also a
 * {@link RouteTarget} in case this segment is also the last which defines a
 * route.
 */
class RouteSegment implements Serializable {

    /**
     * Define a route url parameter details.
     */
    class ParameterDetails extends RouteSegment {

        private boolean optional;

        // Same name as with the segment's name which is extracted by
        // ParameterDetails constructor and provided to the RouteSegment.
        private String name;

        // In case the eligible type is a primitive this is set to the type of
        // the primitive.
        private String eligiblePrimitiveType;

        /**
         * Creates the parameter details using the input segmentPattern pattern.
         * 
         * @param segmentPattern
         *            segment pattern without the preceding colon (:).
         */
        private ParameterDetails(String segmentPattern) {
            optional = segmentPattern.startsWith("[")
                    && segmentPattern.endsWith("]");

            if (optional) {
                segmentPattern = segmentPattern.substring(1,
                        segmentPattern.length() - 1);
            }

            // Extract the pattern defining the value of the parameter.
            final int defStartIndex = segmentPattern.indexOf("<");
            if (defStartIndex != -1 && segmentPattern.endsWith(">")) {

                name = segmentPattern.substring(0, defStartIndex);

                String typeDefPattern = segmentPattern.substring(
                        defStartIndex + 1, segmentPattern.length() - 1);

                extractTypeDef(typeDefPattern);

            } else {
                name = segmentPattern;
            }

        }

        boolean isOptional() {
            return optional;
        }

        boolean isEligible(String value) {
            // We only expect primitive types or as is for now.
            return true;// isPrimitiveEligible(value);
        }

        private boolean isPrimitiveEligible(String value) {
            if (eligiblePrimitiveType != null) {

                if (eligiblePrimitiveType.equals("int")) {

                    try {
                        Integer.valueOf(value);

                        return true;
                    } catch (NumberFormatException e) {
                    }

                } else if (eligiblePrimitiveType.equals("bool")) {
                    if (value.equalsIgnoreCase("true")
                            || value.equalsIgnoreCase("false")) {
                        return true;
                    }
                }

            }

            return false;
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

    class RouteSearchResult {

        // Processed path.
        private String path;

        // Target found for the specified path.
        private RouteTarget target;

        // Parameters found in the path.
        private Map<String, String> urlParameters = new HashMap<>();

        public String getPath() {
            return path;
        }

        public RouteTarget getTarget() {
            return target;
        }

        public Map<String, String> getUrlParameters() {
            return Collections.unmodifiableMap(urlParameters);
        }

        @Override
        public String toString() {
            return "[path: \"" + path + "\", target: " + (target != null
                    ? target.getRoutes().toString()
                    : null) + ", parameters: " + urlParameters + "]";
        }
    }

    /**
     * Create a new root segment instance. This is an empty segment defining the
     * root of the routes tree.
     */
    static RouteSegment createRoot() {
        return new RouteSegment();
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
     * Mapping next segments in the routes by their names.
     */
    private Map<String, RouteSegment> segments;

    /**
     * Mapping next parameter segments in the routes by their names.
     */
    private Map<String, RouteSegment> parameters;

    /**
     * This is valid only is the segment represents a url parameter.
     */
    private ParameterDetails parameterDetails;

    /**
     * Target.
     */
    private RouteTarget target;

    private RouteSegment() {
        this("");
    }

    private RouteSegment(String segment) {
        // In case of a parameter this is stored as the entire input value.
        segmentPattern = segment.trim();

        if (isParameter(segment)) {
            segment = segment.substring(1);

            parameterDetails = new ParameterDetails(segment);

            name = parameterDetails.name;

        } else {
            name = segment;
        }
    }

    String getName() {
        return name;
    }

    String getSegmentPattern() {
        return segmentPattern;
    }

    boolean isParameter() {
        return parameterDetails != null;
    }

    Optional<ParameterDetails> getParameterDetails() {
        return Optional.ofNullable(parameterDetails);
    }

    boolean hasTarget() {
        return target != null;
    }

    Optional<RouteTarget> getTarget() {
        return Optional.ofNullable(target);
    }

    void addPath(String pathPattern,
            Class<? extends Component> targetComponentClass) {
        addPath(pathPattern, () -> new RouteTarget(targetComponentClass));
    }

    /**
     * Add a pathPattern pattern following this route segment. If the pattern
     * already exists and exception is thrown.
     *
     * @param pathPattern
     *            a path pattern where parameters are defined by their ids and
     *            details.
     */
    void addPath(String pathPattern, Supplier<RouteTarget> payloadSupplier) {
        addPath(getSegmentsList(pathPattern), payloadSupplier);
    }

    private void addPath(List<String> segmentPatterns,
            Supplier<RouteTarget> payloadSupplier) {

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
            routeSegment = new RouteSegment(segmentPattern);
            children.put(routeSegment.segmentPattern, routeSegment);
        }

        if (segmentPatterns.size() > 1) {
            routeSegment.addPath(
                    segmentPatterns.subList(1, segmentPatterns.size()),
                    payloadSupplier);

        } else if (!routeSegment.hasTarget()) {
            routeSegment.target = payloadSupplier.get();
        }
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
        RouteSearchResult result = new RouteSearchResult();
        result.path = path;
        result.target = findRouteTarget(getSegmentsList(path),
                result.urlParameters);
        return result;
    }

    private RouteTarget findRouteTarget(List<String> segments,
            Map<String, String> urlParameters) {

        // First try with a static segment (non a parameter). An empty segments
        // list should happen only on root, so this instance should resemble
        // only the root.
        RouteSegment routeSegment = segments.isEmpty() ? this
                : getSegments().get(segments.get(0));

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

            for (RouteSegment parameter : getParameters().values()) {

                RouteTarget target = findRouteTarget(parameter, segments,
                        urlParameters);
                if (target != null) {
                    return target;
                }

                // Try also ignoring the parameter if optional.
                if (parameter.parameterDetails.isOptional()) {
                    HashMap<String, String> outputParameters = new HashMap<>();
                    target = parameter.findRouteTarget(segments,
                            outputParameters);

                    if (target != null) {
                        urlParameters.putAll(outputParameters);
                        return target;
                    }

                }
            }
        }

        return null;
    }

    private RouteTarget findRouteTarget(RouteSegment potentialSegment,
            List<String> segments, Map<String, String> urlParameters) {

        HashMap<String, String> outputParameters = new HashMap<>();

        if (potentialSegment.isParameter()) {
            final String value = segments.get(0);

            if (potentialSegment.parameterDetails.isEligible(value)) {
                outputParameters.put(potentialSegment.getName(), value);

            } else {
                // If the value is not eligible we don't want to go any further.
                return null;
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

    /**
     * Gets the children mapping, either static segments or parameters, which
     * are siblings to segmentPattern.
     */
    private Map<String, RouteSegment> getChildren(String segmentPattern) {
        return isParameter(segmentPattern) ? getParameters() : getSegments();
    }

    private Map<String, RouteSegment> getSegments() {
        if (segments == null) {
            segments = new HashMap<>();
        }

        return segments;
    }

    private Map<String, RouteSegment> getParameters() {
        if (parameters == null) {
            parameters = new HashMap<>();
        }

        return parameters;
    }

    /**
     * Returns a unmodifiable list containing the segments of the specified
     * path.
     */
    private static List<String> getSegmentsList(String path) {
        path = path.trim();

        if (path.startsWith("/")) {
            path = path.substring(1, path.length());
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        final String[] segments = path.split("/");
        if (segments.length == 1 && segments[0].isEmpty()) {
            // This happens on root.
            return Collections.emptyList();

        } else {
            return Arrays.asList(segments);
        }
    }

    private boolean isParameter(String segment) {
        return segment.startsWith(":");
    }

}
