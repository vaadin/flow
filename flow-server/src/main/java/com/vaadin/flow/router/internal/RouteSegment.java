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

        boolean isOptional() {
            return optional;
        }
    }

    class RouteResult {

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
            return "[path: " + path + ", target: " + target.getRoutes()
                    + ", parameters: " + urlParameters + "]";
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

            parameterDetails = new ParameterDetails();
            parameterDetails.optional = segment.startsWith("[")
                    && segment.endsWith("]");

            if (parameterDetails.optional) {
                segment = segment.substring(1, segment.length() - 1);
            }
        }

        name = segment;
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

        String segmentPattern = segmentPatterns.get(0);

        Map<String, RouteSegment> children = getChildren(segmentPattern);

        RouteSegment routeSegment = children.get(segmentPattern);

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
    Optional<RouteResult> findRoute(String path) {
        RouteResult result = new RouteResult();
        result.path = path;
        result.target = findRouteTarget(getSegmentsList(path),
                result.urlParameters);

        if (result.target == null) {
            return Optional.empty();
        } else {
            return Optional.of(result);
        }
    }

    private RouteTarget findRouteTarget(List<String> segments,
            Map<String, String> urlParameters) {

        String segment = segments.get(0);

        RouteSegment routeSegment = getSegments().get(segment);

        if (routeSegment != null) {

            RouteTarget target = findRouteTarget(routeSegment, segments,
                    urlParameters);
            if (target != null) {
                return target;
            }

        } else {
            // Try parameter.
            for (RouteSegment parameter : getParameters().values()) {

                RouteTarget target = findRouteTarget(routeSegment, segments,
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
            outputParameters.put(potentialSegment.getName(), segments.get(0));
        }

        RouteTarget target;

        segments = segments.size() == 1 ? Collections.emptyList()
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

        return Arrays.asList(path.split("/"));
    }

    private boolean isParameter(String segment) {
        return segment.startsWith(":");
    }

}
