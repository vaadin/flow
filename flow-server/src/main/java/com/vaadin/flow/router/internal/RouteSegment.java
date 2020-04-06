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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.AmbiguousRouteConfigurationException;

/**
 * Define a route url segment node with references to its children nodes, thus
 * resembling a recursive tree data structure.
 * <p>
 * A segment can contain a set of the next segment(s) in route(s) and also a
 * {@link RouteTarget} in case this segment is the last in the segments chain
 * referring to its target.ConfiguredRoutes.java
 */
final class RouteSegment implements Serializable {

    /**
     * Name of the segment.
     */
    private String name;

    /**
     * Segment template string as provided in constructor. This is used
     * internally as a key in the parent's mapping, to make clear distinction
     * between static segment values and parameters which are defined as a
     * template used to extract the value from a url path.
     */
    private String template;

    /**
     * Parameter details.
     */
    private RouteFormat.ParameterInfo info;

    /**
     * Parameter matching regex.
     */
    private Pattern pattern;

    /**
     * Target.
     */
    private RouteTarget target;

    /**
     * Mapping next segments in the routes by the segment template.
     */
    private Map<String, RouteSegment> staticSegments;

    /**
     * Mapping next parameter segments in the routes by the segment template.
     */
    private Map<String, RouteSegment> parameterSegments;

    /**
     * Mapping next optional parameter segments in the routes by the segment
     * template.
     */
    private Map<String, RouteSegment> optionalSegments;

    /**
     * Mapping varargs parameter segments in the routes by the segment template.
     */
    private Map<String, RouteSegment> varargsSegments;

    /**
     * Track the mapping of all segment types in the routes by the segment
     * template.
     */
    private Map<String, RouteSegment> allSegments;

    private RouteSegment() {
    }

    private RouteSegment(String segmentTemplate) {
        this.template = segmentTemplate;

        if (RouteFormat.isParameter(segmentTemplate)) {
            info = new RouteFormat.ParameterInfo(segmentTemplate);

            final String regex = getRegex();
            if (!regex.isEmpty()) {
                pattern = Pattern.compile(regex);
            }

            this.name = info.getName();
        } else {
            this.name = segmentTemplate;
        }
    }

    public RouteSegment(RouteSegment original) {
        this.name = original.name;
        this.template = original.template;
        this.info = original.info;
        this.pattern = original.pattern;
        this.target = original.target;

        original.getStaticSegments().entrySet()
                .forEach(e -> this.addSegment(new RouteSegment(e.getValue()),
                        this.getStaticSegments()));

        original.getParameterSegments().entrySet()
                .forEach(e -> this.addSegment(new RouteSegment(e.getValue()),
                        this.getParameterSegments()));

        original.getOptionalSegments().entrySet()
                .forEach(e -> this.addSegment(new RouteSegment(e.getValue()),
                        this.getOptionalSegments()));

        original.getVarargsSegments().entrySet()
                .forEach(e -> this.addSegment(new RouteSegment(e.getValue()),
                        this.getVarargsSegments()));
    }

    /**
     * Create a new root segment instance.
     */
    static RouteSegment createRoot() {
        return new RouteSegment("");
    }

    String getName() {
        return name;
    }

    String getTemplate() {
        return template;
    }

    boolean hasTarget() {
        return target != null;
    }

    RouteTarget getTarget() {
        return target;
    }

    boolean isParameter() {
        return info != null;
    }

    String getRegex() {
        return isParameter() ? info.getRegex() : null;
    }

    boolean isOptional() {
        return isParameter() && info.isOptional();
    }

    boolean isVarargs() {
        return isParameter() && info.isVarargs();
    }

    boolean isMandatory() {
        return !isOptional() && !isVarargs();
    }

    boolean isEligible(String value) {
        if (!isParameter()) {
            return Objects.equals(getName(), value);
        }

        if (getRegex().isEmpty()) {
            return true;
        }

        if (pattern != null) {
            return pattern.matcher(value).matches();
        }

        return false;
    }

    /**
     * Collects all routes in an unmodifiable {@link Map}.
     *
     * @return a {@link Map} containing all url templates and their specific
     *         targets.
     */
    Map<String, RouteTarget> getRoutes() {

        Map<String, RouteTarget> result = new LinkedHashMap<>();

        if (target != null) {
            result.put("", target);
        }

        collectRoutes(result, getStaticSegments());
        collectRoutes(result, getParameterSegments());
        collectRoutes(result, getOptionalSegments());
        collectRoutes(result, getVarargsSegments());

        if (getTemplate().isEmpty()) {
            return Collections.unmodifiableMap(result);
        } else {
            return result;
        }
    }

    void removeSubRoute(String urlTemplate) {
        removeSubRoute(PathUtil.getSegmentsList(urlTemplate));
    }

    /**
     * Add a urlTemplate template following this route segment. If the template
     * already exists and exception is thrown.
     *
     * @param urlTemplate
     *            a url template where parameters are defined by their ids and
     *            details.
     * @param target
     *            target to set for the given url template
     */
    void addSubRoute(String urlTemplate, RouteTarget target) {
        addSubRoute(PathUtil.getSegmentsList(urlTemplate), target);
    }

    /**
     * Search for a route target using given navigation <code>url</code>
     * argument.
     *
     * @param url
     *            the navigation url used to search a route target.
     * @return a {@link NavigationRouteTarget} instance containing the
     *         {@link RouteTarget} and {@link RouteParameters} extracted from the
     *         <code>url</code> argument according with the route configuration.
     */
    NavigationRouteTarget getNavigationRouteTarget(String url) {

        Map<String, String> parameters = new HashMap<>();

        RouteTarget routeTarget = url == null ? null
                : findRouteTarget(PathUtil.getSegmentsList(url), parameters);

        return new NavigationRouteTarget(url, routeTarget, parameters);
    }

    /**
     * Gets a simple representation of the path tamplate.
     *
     * @param urlTemplate
     *            the full url template.
     * @param parameterFormat
     *            the parameter format function.
     * @return the simple url template.
     */
    String formatUrlTemplate(String urlTemplate,
            Function<RouteSegment, String> parameterFormat) {
        if (urlTemplate == null) {
            return null;
        }

        final List<String> result = new ArrayList<>();

        matchSegmentTemplates(urlTemplate,
                routeSegment -> result.add(routeSegment.isParameter()
                        ? parameterFormat.apply(routeSegment)
                        : routeSegment.getName()),
                null);

        if (result.isEmpty()) {
            return "";
        } else {
            return String.join("/", result);
        }
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

    private void removeSubRoute(List<String> segmentPatterns) {
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
                routeSegment.removeSubRoute(
                        segmentPatterns.subList(1, segmentPatterns.size()));
            } else {
                routeSegment.target = null;
            }

            if (routeSegment.isEmpty() && routeSegment != this) {
                removeSegment(segmentPattern, children);
            }
        }
    }

    private void addSubRoute(List<String> segmentPatterns, RouteTarget target) {

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
            if (RouteFormat.isVarargsParameter(segmentPattern)
                    && segmentPatterns.size() > 1) {
                throw new IllegalArgumentException(
                        "A varargs url parameter can be defined only as the last path segment");
            }

            // We reject any route where the last segment is an optional
            // parameter while there's already a target set for the same
            // route without the optional parameter.
            if (RouteFormat.isOptionalParameter(segmentPattern)
                    && segmentPatterns.size() == 1 && hasTarget()) {
                throw ambigousOptionalTarget(target.getTarget(),
                        getTarget().getTarget());
            }

            routeSegment = addSegment(segmentPattern, children);
        }

        routeSegment.setRouteTarget(segmentPatterns, target);
    }

    private void setRouteTarget(List<String> segmentPatterns,
            RouteTarget target) {
        if (segmentPatterns.size() > 1) {
            addSubRoute(segmentPatterns.subList(1, segmentPatterns.size()),
                    target);

        } else {
            if (!hasTarget()) {

                // We reject any route where there's already a target set
                // for the same route with an optional.
                RouteSegment optional = getOptionalParameterWithTarget();
                if (optional != null) {
                    throw optional.ambigousOptionalTarget(
                            optional.getTarget().getTarget(),
                            target.getTarget());
                }

                this.target = target;

            } else {
                throw ambigousTarget(target.getTarget());
            }
        }
    }

    private RouteTarget findRouteTarget(List<String> segments,
            Map<String, String> parameters) {

        // First try with a static segment (non a parameter). An empty
        // segments list should happen only on root, so this instance should
        // resemble only the root.
        RouteSegment routeSegment = segments.isEmpty() ? this
                : getStaticSegments().get(segments.get(0));

        // Static segments
        if (routeSegment != null) {
            RouteTarget foundTarget = routeSegment
                    .getRouteTargetMatchingParameter(segments, parameters);
            if (foundTarget != null) {
                return foundTarget;
            }
        }

        // If no route following a static segment was found try through
        // parameters.
        if (!segments.isEmpty()) {

            RouteTarget foundTarget;

            // Mandatory parameters
            foundTarget = findRouteTarget(segments, parameters,
                    getParameterSegments());
            if (foundTarget != null) {
                return foundTarget;
            }

            // Optionals
            foundTarget = findRouteTarget(segments, parameters,
                    getOptionalSegments());
            if (foundTarget != null) {
                return foundTarget;
            }

            // Optional's children
            foundTarget = findRouteTargetInOptionals(segments, parameters);
            if (foundTarget != null) {
                return foundTarget;
            }

            // Varargs
            foundTarget = findRouteTarget(segments, parameters,
                    getVarargsSegments());
            if (foundTarget != null) {
                return foundTarget;
            }
        }

        return null;
    }

    private RouteTarget findRouteTargetInOptionals(List<String> segments,
            Map<String, String> parameters) {
        RouteTarget foundTarget;
        for (RouteSegment parameter : getOptionalSegments().values()) {
            // Try ignoring the parameter if optional and look into its
            // children using the same segments.
            Map<String, String> outputParameters = new HashMap<>();
            foundTarget = parameter.findRouteTarget(segments, outputParameters);

            if (foundTarget != null) {
                parameters.putAll(outputParameters);
                return foundTarget;
            }
        }
        return null;
    }

    private RouteTarget findRouteTarget(List<String> segments,
            Map<String, String> parameters,
            Map<String, RouteSegment> children) {
        for (RouteSegment segment : children.values()) {
            RouteTarget foundTarget = segment
                    .getRouteTargetMatchingParameter(segments, parameters);
            if (foundTarget != null) {
                return foundTarget;
            }
        }
        return null;
    }

    private RouteTarget getRouteTargetMatchingParameter(List<String> segments,
            Map<String, String> parameters) {

        Map<String, String> outputParameters = new HashMap<>();

        // Handle varargs.
        if (isVarargs()) {

            for (String value : segments) {
                if (!isEligible(value)) {
                    // If any value is not eligible we don't want to go
                    // any further.
                    return null;
                }
            }

            outputParameters.put(getName(), PathUtil.getPath(segments));
            segments = Collections.emptyList();

        } else if (isParameter()) {
            // Handle one parameter value.
            String value = segments.get(0);

            if (isEligible(value)) {
                outputParameters.put(getName(), value);

            } else {
                // If the value is not eligible we don't want to go any
                // further.
                return null;
            }
        }

        segments = segments.size() <= 1 ? Collections.emptyList()
                : segments.subList(1, segments.size());

        RouteTarget foundTarget = getRouteTarget(segments, outputParameters);

        if (foundTarget != null) {
            parameters.putAll(outputParameters);
        }

        return foundTarget;
    }

    private RouteTarget getRouteTarget(List<String> segments,
            Map<String, String> outputParameters) {
        RouteTarget foundTarget;
        if (!segments.isEmpty()) {
            // Continue looking if there any more segments.
            foundTarget = findRouteTarget(segments, outputParameters);

        } else if (hasTarget()) {
            // Found target.
            foundTarget = getTarget();

        } else {
            // Look for target in optional children.
            RouteSegment optionalChild = getAnyOptionalOrVarargsParameterWithTarget();
            if (optionalChild != null) {
                foundTarget = optionalChild.getTarget();
            } else {
                foundTarget = null;
            }
        }
        return foundTarget;
    }

    void matchSegmentTemplates(String urlTemplate,
            Consumer<RouteSegment> segmentProcessor,
            Consumer<RouteSegment> targetSegmentProcessor) {
        matchSegmentTemplates(urlTemplate,
                PathUtil.getSegmentsList(urlTemplate), segmentProcessor,
                targetSegmentProcessor);
    }

    private void matchSegmentTemplates(final String urlTemplate,
            List<String> segmentTemplates,
            Consumer<RouteSegment> segmentProcessor,
            Consumer<RouteSegment> targetSegmentProcessor) {
        if (segmentTemplates.isEmpty()) {
            return;
        }

        RouteSegment routeSegment = getAllSegments()
                .get(segmentTemplates.get(0));

        if (routeSegment == null) {
            throw new IllegalArgumentException(
                    "Unregistered url template \"" + urlTemplate + "\"");
        }

        if (segmentProcessor != null) {
            segmentProcessor.accept(routeSegment);
        }

        if (segmentTemplates.size() > 1) {
            routeSegment.matchSegmentTemplates(urlTemplate,
                    segmentTemplates.subList(1, segmentTemplates.size()),
                    segmentProcessor, targetSegmentProcessor);

        } else if (routeSegment.getTarget() == null) {
            throw new IllegalArgumentException(
                    "Unregistered url template \"" + urlTemplate + "\"");

        } else if (targetSegmentProcessor != null) {
            targetSegmentProcessor.accept(routeSegment);
        }
    }

    void matchSegmentTemplatesWithParameters(String urlTemplate,
            RouteParameters parameters,
            Consumer<RouteSegmentValue> segmentProcessor,
            Consumer<RouteSegment> targetSegmentProcessor) {

        final List<String> segmentTemplates = PathUtil
                .getSegmentsList(urlTemplate);

        if (segmentTemplates.isEmpty()
                && parameters.getParameterNames().isEmpty()) {
            return;
        }

        final Set<String> parameterNames = new HashSet<>(
                parameters.getParameterNames());

        RouteParameters finalParameters = parameters;
        matchSegmentTemplates(urlTemplate, segmentTemplates, routeSegment -> {
            final Optional<String> segmentValue = getSegmentValue(routeSegment,
                    finalParameters);

            if (routeSegment.isParameter()) {
                parameterNames.remove(routeSegment.getName());
            }

            if (segmentProcessor != null) {
                segmentProcessor.accept(
                        new RouteSegmentValue(routeSegment, segmentValue));
            }
        }, routeSegment -> {
            // All parameter must be used.
            if (!parameterNames.isEmpty()) {
                throw new IllegalArgumentException(
                        "All provided RouteParameters must be used to process the urlTemplate. Provide the exact required RouteParameters or a urlTemplate that will use all RouteParameters");
            }

            if (targetSegmentProcessor != null) {
                targetSegmentProcessor.accept(routeSegment);
            }
        });
    }

    /**
     * Returns any optional or varargs (since that's optional too) parameter
     * child with a target set so in case there's no target on a potential
     * targeted segment we use the target from the optional child. The search is
     * performed recursively on this segment.
     */
    private RouteSegment getAnyOptionalOrVarargsParameterWithTarget() {
        RouteSegment optionalParameter = getOptionalParameterWithTarget();
        if (optionalParameter != null) {
            return optionalParameter;
        }

        // Try looking into children.
        for (RouteSegment parameter : getOptionalSegments().values()) {
            return parameter.getAnyOptionalOrVarargsParameterWithTarget();
        }

        // Look for the first vararg.
        final Map<String, RouteSegment> varargsParameters = getVarargsSegments();
        if (!varargsParameters.isEmpty()) {
            return varargsParameters.values().iterator().next();

        } else {
            return null;
        }
    }

    /**
     * Returns a child optional parameter with target.
     */
    private RouteSegment getOptionalParameterWithTarget() {
        for (RouteSegment parameter : getOptionalSegments().values()) {
            if (parameter.hasTarget()) {
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

    private RuntimeException ambigousTarget(Class<? extends Component> target) {

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

    boolean isEmpty() {
        return target == null && getAllSegments().isEmpty();
    }

    private RouteSegment addSegment(String segmentTemplate,
            Map<String, RouteSegment> children) {
        RouteSegment routeSegment = new RouteSegment(segmentTemplate);
        addSegment(routeSegment, children);
        return routeSegment;
    }

    private void addSegment(RouteSegment routeSegment,
            Map<String, RouteSegment> children) {
        children.put(routeSegment.getTemplate(), routeSegment);
        getAllSegments().put(routeSegment.getTemplate(), routeSegment);
    }

    private void removeSegment(String segmentTemplate,
            Map<String, RouteSegment> children) {
        children.remove(segmentTemplate);
        getAllSegments().remove(segmentTemplate);
    }

    /**
     * Gets the children mapping, either static segments or parameters, which
     * are siblings to segmentPattern.
     */
    private Map<String, RouteSegment> getChildren(String segmentPattern) {
        return RouteFormat.isVarargsParameter(segmentPattern)
                ? getVarargsSegments()
                : RouteFormat.isOptionalParameter(segmentPattern)
                        ? getOptionalSegments()
                        : RouteFormat.isParameter(segmentPattern)
                                ? getParameterSegments()
                                : getStaticSegments();
    }

    private Map<String, RouteSegment> getStaticSegments() {
        if (staticSegments == null) {
            // We want to process the data according to the priority and
            // registration order.
            staticSegments = new LinkedHashMap<>();
        }
        return staticSegments;
    }

    private Map<String, RouteSegment> getParameterSegments() {
        if (parameterSegments == null) {
            // We want to process the data according to the priority and
            // registration order.
            parameterSegments = new LinkedHashMap<>();
        }
        return parameterSegments;
    }

    private Map<String, RouteSegment> getOptionalSegments() {
        if (optionalSegments == null) {
            // We want to process the data according to the priority and
            // registration order.
            optionalSegments = new LinkedHashMap<>();
        }
        return optionalSegments;
    }

    private Map<String, RouteSegment> getVarargsSegments() {
        if (varargsSegments == null) {
            // We want to process the data according to the priority and
            // registration order.
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

    private static Optional<String> getSegmentValue(RouteSegment routeSegment,
            RouteParameters parameters) {

        if (routeSegment.isVarargs()) {
            return getVarargsValue(routeSegment, parameters);

        } else if (routeSegment.isParameter()) {
            return getParameterValue(routeSegment, parameters);

        } else {
            return Optional.of(routeSegment.getName());
        }
    }

    private static Optional<String> getVarargsValue(RouteSegment routeSegment,
            RouteParameters parameters) {
        final String parameterName = routeSegment.getName();

        List<String> args = parameters.getWildcard(parameterName);

        final List<String> result = new ArrayList<>(args.size());

        for (String value : args) {
            if (!routeSegment.isEligible(value)) {
                throw new IllegalArgumentException("Url varargs parameter `"
                        + parameterName + "` has a specified value `" + value
                        + "`, which is invalid according to the parameter definition `"
                        + routeSegment.getTemplate() + "`");
            }

            result.add(value);
        }

        // Varargs are always last so no need to even try going
        // forward.
        final String path = PathUtil.getPath(result);
        return path.isEmpty() ? Optional.empty() : Optional.of(path);
    }

    private static Optional<String> getParameterValue(RouteSegment routeSegment,
            RouteParameters parameters) {
        final String parameterName = routeSegment.getName();

        final Optional<String> value = parameters.get(parameterName);

        if (!value.isPresent() && routeSegment.isMandatory()) {
            throw new IllegalArgumentException("Url parameter `" + parameterName
                    + "` is mandatory but missing from the parameters argument.");
        }

        if (value.isPresent() && !routeSegment.isEligible(value.get())) {
            throw new IllegalArgumentException("Url parameter `" + parameterName
                    + "` has specified value `" + value
                    + "`, which is invalid according to the parameter definition `"
                    + routeSegment.getTemplate() + "`");
        }

        return value;
    }

}
