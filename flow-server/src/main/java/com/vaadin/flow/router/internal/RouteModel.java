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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouteParameterFormat;
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

    private RouteSegment root;

    private RouteModel() {
        this(RouteSegment.createRoot());
    }

    private RouteModel(RouteSegment root) {
        this.root = root;
    }

    /**
     * Create a new root segment instance. This is an empty segment defining the
     * root of the routes tree.
     */
    static RouteModel create() {
        return new RouteModel();
    }

    /**
     * Copy the given model into a new one.
     * 
     * @param original
     *            the original model.
     * @return a copy of the original model.
     */
    static RouteModel copy(RouteModel original) {
        return new RouteModel(original.root.copy());
    }

    /**
     * Define a route url segment tree data model which is used to store
     * internally registered routes.
     * <p>
     * A segment may contain a set of the next segment(s) in route(s) and also a
     * {@link RouteTarget} in case this segment is also the last which defines a
     * route.
     */
    static final class RouteSegment implements Serializable {

        /**
         * Name of the segment.
         */
        private String name;

        /**
         * Segment template string as provided in constructor. This is used
         * internally as a key in the parent's mapping, to make clear
         * distinction between static segment values and parameters which are
         * defined as a template used to extract the value from a url path.
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
         * Mapping next parameter segments in the routes by the segment
         * template.
         */
        private Map<String, RouteSegment> parameterSegments;

        /**
         * Mapping next optional parameter segments in the routes by the segment
         * template.
         */
        private Map<String, RouteSegment> optionalSegments;

        /**
         * Mapping varargs parameter segments in the routes by the segment
         * template.
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

                if (!isPrimitiveType()) {
                    pattern = Pattern.compile(getType());
                }

                this.name = info.getName();
            } else {
                this.name = segmentTemplate;
            }
        }

        /**
         * Create a new root segment instance. This is an empty segment defining
         * the root of the routes tree.
         */
        static RouteSegment createRoot() {
            return new RouteSegment("");
        }

        RouteSegment copy() {
            final RouteSegment clone = new RouteSegment();

            clone.name = name;
            clone.template = template;
            clone.info = info;
            clone.pattern = pattern;
            clone.target = target;

            getStaticSegments().entrySet()
                    .forEach(e -> clone.addSegment(e.getValue().copy(),
                            clone.getStaticSegments()));

            getParameterSegments().entrySet()
                    .forEach(e -> clone.addSegment(e.getValue().copy(),
                            clone.getParameterSegments()));

            getOptionalSegments().entrySet()
                    .forEach(e -> clone.addSegment(e.getValue().copy(),
                            clone.getOptionalSegments()));

            getVarargsSegments().entrySet()
                    .forEach(e -> clone.addSegment(e.getValue().copy(),
                            clone.getVarargsSegments()));

            return clone;
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

        private boolean isParameter() {
            return info != null;
        }

        boolean isPrimitiveType() {
            return isParameter()
                    ? RouteFormat.PRIMITIVE_TYPES.contains(getType())
                    : false;
        }

        String getType() {
            return isParameter() ? info.getRegex() : null;
        }

        String getTypeAsPrimitive() {
            return isPrimitiveType() ? getType() : RouteFormat.STRING_TEMPLATE;
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

            if (getType().isEmpty()) {
                return true;
            }

            if (pattern != null) {
                return pattern.matcher(value).matches();
            }

            if (RouteFormat.INT_TEMPLATE.equals(getType())) {
                try {
                    Integer.valueOf(value);
                    return true;
                } catch (NumberFormatException e) {
                }

            } else if (RouteFormat.LONG_TEMPLATE.equals(getType())) {
                try {
                    Long.valueOf(value);
                    return true;
                } catch (NumberFormatException e) {
                }

            } else if (RouteFormat.BOOL_TYPE.equals(getType())) {
                if (value.equalsIgnoreCase("true")
                        || value.equalsIgnoreCase("false")) {
                    return true;
                }

            } else if (RouteFormat.STRING_TEMPLATE.equals(getType())) {
                return true;
            }

            return false;
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
         * Add a urlTemplate template following this route segment. If the
         * template already exists and exception is thrown.
         *
         * @param urlTemplate
         *            a path template where parameters are defined by their ids
         *            and details.
         * @param target
         *            target to set for the given path template
         */
        void addSubRoute(String urlTemplate, RouteTarget target) {
            addSubRoute(PathUtil.getSegmentsList(urlTemplate), target);
        }

        /**
         * Finds a route for the given url.
         *
         * @param url
         *            navigation url where the parameters are provided with
         *            their real value. The method is looking to map the value
         *            provided in the url with the ids found in the stored
         *            templates.
         * @return a route result containing the target and parameter values
         *         mapped by their ids.
         */
        NavigationRouteTarget getNavigationRouteTarget(String url) {

            Map<String, String> urlParameters = new HashMap<>();

            RouteTarget target = url == null ? null
                    : findRouteTarget(PathUtil.getSegmentsList(url),
                            urlParameters);

            return new NavigationRouteTarget(url, target, urlParameters);
        }

        /**
         * Gets a simple representation of the path tamplate.
         * 
         * @param urlTemplate
         *            the full path template.
         * @param parameterFormat
         *            the parameter format function.
         * @return the simple path template.
         */
        String getUrlTemplate(String urlTemplate,
                Function<RouteSegment, String> parameterFormat) {
            if (urlTemplate == null) {
                return null;
            }

            final List<String> segments = PathUtil.getSegmentsList(urlTemplate);
            final List<String> result = new ArrayList<>(segments.size());

            matchSegmentTemplates(segments, routeSegment -> {
                result.add(routeSegment.isParameter()
                        ? parameterFormat.apply(routeSegment)
                        : routeSegment.getName());
            }, null);

            if (result.isEmpty()) {
                return "";
            } else {
                return String.join("/", result);
            }
        }

        String getUrl(String urlTemplate, UrlParameters parameters) {
            if (urlTemplate == null) {
                return null;
            }

            final List<String> segments = PathUtil.getSegmentsList(urlTemplate);

            if (segments.isEmpty() && parameters.getParameters().size() == 0) {
                return "";
            }

            final List<String> result = new ArrayList<>(segments.size());

            matchSegmentTemplates(segments, routeSegment -> {
                String segment = routeSegment.getTemplate();

                final String parameterName = routeSegment.getName();

                if (routeSegment.isVarargs()) {
                    List<String> args = parameters.getSegments(parameterName);

                    if (args == null) {
                        final String value = parameters.get(parameterName);
                        if (value != null) {
                            args = Collections.singletonList(value);
                        } else {
                            args = Collections.emptyList();
                        }
                    }

                    for (String value : args) {
                        if (!routeSegment.isEligible(value)) {
                            throw new IllegalArgumentException(
                                    "Url varargs parameter `" + parameterName
                                            + "` has a specified value `"
                                            + value
                                            + "`, which is invalid according to the parameter definition `"
                                            + segment + "`");
                        }

                        result.add(value);
                    }

                    // Varargs are always last so no need to even try going
                    // forward.
                    return;

                } else if (routeSegment.isParameter()) {
                    final String value = parameters.get(parameterName);

                    if (value == null && routeSegment.isMandatory()) {
                        throw new IllegalArgumentException("Url parameter `"
                                + parameterName
                                + "` is mandatory but missing from the parameters argument.");
                    }

                    if (value != null && !routeSegment.isEligible(value)) {
                        throw new IllegalArgumentException("Url parameter `"
                                + parameterName + "` has specified value `"
                                + value
                                + "`, which is invalid according to the parameter definition `"
                                + segment + "`");
                    }

                    if (value != null) {
                        result.add(value);
                    }

                } else {
                    result.add(segment);
                }

            }, null);

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

        private void addSubRoute(List<String> segmentPatterns,
                RouteTarget target) {

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
                            "A varargs url parameter may be defined only as the last path segment");
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

            addSubRoute(routeSegment, segmentPatterns, target);
        }

        private void addSubRoute(RouteSegment potentialSegment,
                List<String> segmentPatterns, RouteTarget target) {
            if (segmentPatterns.size() > 1) {
                potentialSegment.addSubRoute(
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
                Map<String, String> urlParameters) {

            // First try with a static segment (non a parameter). An empty
            // segments list should happen only on root, so this instance should
            // resemble only the root.
            RouteSegment routeSegment = segments.isEmpty() ? this
                    : getStaticSegments().get(segments.get(0));

            if (routeSegment != null) {
                RouteTarget foundTarget = findRouteTarget(routeSegment,
                        segments, urlParameters);
                if (foundTarget != null) {
                    return foundTarget;
                }
            }

            // If no route following a static segment was found try through
            // parameters.
            if (!segments.isEmpty()) {

                for (RouteSegment parameter : getParameterSegments().values()) {
                    RouteTarget foundTarget = findRouteTarget(parameter,
                            segments, urlParameters);
                    if (foundTarget != null) {
                        return foundTarget;
                    }
                }

                for (RouteSegment parameter : getOptionalSegments().values()) {
                    RouteTarget foundTarget = findRouteTarget(parameter,
                            segments, urlParameters);
                    if (foundTarget != null) {
                        return foundTarget;
                    }
                }

                for (RouteSegment parameter : getOptionalSegments().values()) {
                    // Try ignoring the parameter if optional and look into its
                    // children using the same segments.
                    Map<String, String> outputParameters = new HashMap<>();
                    RouteTarget foundTarget = parameter
                            .findRouteTarget(segments, outputParameters);

                    if (foundTarget != null) {
                        urlParameters.putAll(outputParameters);
                        return foundTarget;
                    }
                }

                for (RouteSegment varargParameter : getVarargsSegments()
                        .values()) {
                    RouteTarget foundTarget = findRouteTarget(varargParameter,
                            segments, urlParameters);
                    if (foundTarget != null) {
                        return foundTarget;
                    }
                }
            }

            return null;
        }

        private RouteTarget findRouteTarget(RouteSegment potentialSegment,
                List<String> segments, Map<String, String> urlParameters) {

            Map<String, String> outputParameters = new HashMap<>();

            // Handle varargs.
            if (potentialSegment.isVarargs()) {

                for (String value : segments) {
                    if (!potentialSegment.isEligible(value)) {
                        // If any value is not eligible we don't want to go
                        // any further.
                        return null;
                    }
                }

                outputParameters.put(potentialSegment.getName(),
                        PathUtil.getPath(segments));
                segments = Collections.emptyList();

            } else if (potentialSegment.isParameter()) {
                // Handle one parameter value.
                String value = segments.get(0);

                if (potentialSegment.isEligible(value)) {
                    outputParameters.put(potentialSegment.getName(), value);

                } else {
                    // If the value is not eligible we don't want to go any
                    // further.
                    return null;
                }
            }

            RouteTarget foundTarget;

            segments = segments.size() <= 1 ? Collections.emptyList()
                    : segments.subList(1, segments.size());

            if (!segments.isEmpty()) {
                // Continue looking if there any more segments.
                foundTarget = potentialSegment.findRouteTarget(segments,
                        outputParameters);

            } else if (potentialSegment.hasTarget()) {
                // Found target.
                foundTarget = potentialSegment.getTarget();

            } else {
                // Look for target in optional children.
                RouteSegment optionalChild = potentialSegment
                        .getAnyOptionalOrVarargsParameterWithTarget();
                if (optionalChild != null) {
                    foundTarget = optionalChild.getTarget();
                } else {
                    foundTarget = null;
                }
            }

            if (foundTarget != null) {
                urlParameters.putAll(outputParameters);
            }

            return foundTarget;
        }

        void matchSegmentTemplates(List<String> segmentTemplates,
                Consumer<RouteSegment> segmentProcessor,
                Consumer<RouteSegment> targetSegmentProcessor) {
            if (segmentTemplates.isEmpty()) {
                return;
            }

            RouteSegment routeSegment = getAllSegments()
                    .get(segmentTemplates.get(0));

            if (routeSegment == null) {
                throw new IllegalArgumentException(
                        "Unregistered path template specified `"
                                + PathUtil.getPath(segmentTemplates) + "`");
            }

            if (segmentProcessor != null) {
                segmentProcessor.accept(routeSegment);
            }

            if (segmentTemplates.size() > 1) {
                routeSegment.matchSegmentTemplates(
                        segmentTemplates.subList(1, segmentTemplates.size()),
                        segmentProcessor, targetSegmentProcessor);

            } else if (routeSegment.getTarget() == null) {
                throw new IllegalArgumentException(
                        "Missing target at the end of the path `"
                                + PathUtil.getPath(segmentTemplates) + "`");

            } else if (targetSegmentProcessor != null) {
                targetSegmentProcessor.accept(routeSegment);
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
         * Gets the children mapping, either static segments or parameters,
         * which are siblings to segmentPattern.
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

        private Map<String, RouteSegment> getOptionalSegments() {
            if (optionalSegments == null) {
                // Parameters iteration must be based on insertion.
                optionalSegments = new LinkedHashMap<>();
            }
            return optionalSegments;
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

    /**
     * Collects all routes in an unmodifiable {@link Map}.
     *
     * @return a {@link Map} containing all paths and their specific targets.
     */
    Map<String, RouteTarget> getRoutes() {
        return root.getRoutes();
    }

    /**
     * Remove a path by it's url template.
     * 
     * @param urlTemplate
     *            the full url template.
     */
    void removeRoute(String urlTemplate) {
        root.removeSubRoute(urlTemplate);
    }

    /**
     * Add a urlTemplate template following this route segment. If the template
     * already exists and exception is thrown.
     *
     * @param urlTemplate
     *            a path template where parameters are defined by their ids and
     *            details.
     * @param target
     *            target to set for the given path template.
     * @throws InvalidRouteConfigurationException
     *             if the combination of urlTemplate and target doesn't make
     *             send within the current state of the model.
     * @throws IllegalArgumentException
     *             in case the varargs are specified in the middle of the
     *             urlTemplate. Varargs may be specified only as the last
     *             segment definition.
     */
    void addRoute(String urlTemplate, RouteTarget target) {
        root.addSubRoute(urlTemplate, target);
    }

    /**
     * Finds a route target for the given url.
     *
     * @param url
     *            navigation url where the parameters are provided with their
     *            real value. The method is looking to map the value provided in
     *            the url with the ids found in the stored templates.
     * @return a route result containing the target and parameter values mapped
     *         by their ids.
     */
    NavigationRouteTarget getNavigationRouteTarget(String url) {
        return root.getNavigationRouteTarget(url);
    }

    /**
     * Finds a route target for the given urlTemplate and parameters.
     *
     * @param urlTemplate
     *            the full path template.
     * @param parameters
     *            the parameters to use or null if no parameters specified.
     * @return a route result containing the target and parameter values mapped
     *         by their ids.
     * @throws IllegalArgumentException
     *             in case urlTemplate is not registered or the parameters do
     *             not match with the template.
     */
    RouteTarget getRouteTarget(String urlTemplate, UrlParameters parameters) {
        AtomicReference<RouteTarget> target = new AtomicReference<>();
        root.matchSegmentTemplates(PathUtil.getSegmentsList(urlTemplate), null,
                routeSegment -> {
                    target.set(routeSegment.getTarget());
                });
        return target.get();
    }

    /**
     * Gets a url path by replacing into the path template the url parameters.
     * <p>
     * In case all parameters defined in the urlTemplate are optional or
     * varargs, parameters argument may be null and the path will be provided
     * without any parameters.
     * 
     * @param urlTemplate
     *            the full path template.
     * @param parameters
     *            the parameters to use or null if no parameters specified.
     * @return the url.
     * @throws IllegalArgumentException
     *             in case urlTemplate is not registered or the parameters do
     *             not match with the template.
     */
    String getUrl(String urlTemplate, UrlParameters parameters) {
        return root.getUrl(urlTemplate,
                parameters != null ? parameters : new UrlParameters());
    }

    /**
     * Format the url template using the given format settings.
     * 
     * @param urlTemplate
     *            the urlTemplate.
     * @param format
     *            the new format to use.
     * @return a String representing the urlTemplate in the given format.
     * @throws IllegalArgumentException
     *             in case urlTemplate is not registered or the parameters do
     *             not match with the template.
     */
    String formatUrlTemplate(String urlTemplate,
            Set<RouteParameterFormat> format) {

        if (format.contains(RouteParameterFormat.TEMPLATE)) {
            return urlTemplate;
        }

        return root.getUrlTemplate(urlTemplate, segment -> {
            StringBuilder result = new StringBuilder();

            result.append(":");
            
            final boolean containsType = format
                    .contains(RouteParameterFormat.SIMPLE_TYPE)
                    || format.contains(RouteParameterFormat.TYPE)
                    || format.contains(RouteParameterFormat.CAPITALIZED_TYPE);
            boolean closeTypeBracket = false;

            if (format.contains(RouteParameterFormat.NAME)) {
                result.append(segment.getName());
                if (containsType) {
                    result.append("(");
                    closeTypeBracket = true;
                }
            }

            if (containsType) {
                String type = formatSegmentType(segment, format);

                result.append(type);

                if (closeTypeBracket) {
                    result.append(")");
                }
            }

            return result.toString();
        });
    }

    /**
     * Gets the parameters found in the given urlTemplate. The result contains
     * the names of the parameters as keys and the values represent the
     * parameter template formatted accordingly to the given format.
     * 
     * @param urlTemplate
     *            the url template.
     * @param format
     *            format the values of the result {@link Map}
     * @return a {@link Map} containing the names of the parameters mapped by
     *         their formatted template using the given format.
     * @throws IllegalArgumentException
     *             in case urlTemplate is not registered or the parameters do
     *             not match with the template.
     */
    Map<String, String> getParameters(String urlTemplate,
            EnumSet<RouteParameterFormat> format) {
        Map<String, String> result = new HashMap<>();

        this.root.matchSegmentTemplates(PathUtil.getSegmentsList(urlTemplate),
                segment -> {
                    if (segment.isParameter()) {
                        result.put(segment.getName(),
                                formatSegmentType(segment, format));
                    }
                }, null);
        return result;
    }

    private String formatSegmentType(RouteSegment segment,
            Set<RouteParameterFormat> format) {
        String type = segment.getType();

        if (format.contains(RouteParameterFormat.SIMPLE_TYPE)
                || segment.isPrimitiveType()) {

            if (!segment.isPrimitiveType()) {
                type = "string";
            }

            if (format.contains(RouteParameterFormat.CAPITALIZED_TYPE)) {
                type = capitalize(type);
            }
        }
        return type;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

}
