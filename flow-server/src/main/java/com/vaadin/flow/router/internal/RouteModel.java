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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.flow.router.RouteParameterData;
import com.vaadin.flow.router.RouteParameterFormatOption;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.InvalidRouteConfigurationException;

/**
 * Define a route url template data model which is used to store internally
 * registered routes.
 */
class RouteModel implements Serializable {

    private boolean mutable;

    private RouteSegment root;

    private RouteModel(boolean mutable) {
        this(RouteSegment.createRoot(), mutable);
    }

    private RouteModel(RouteSegment root, boolean mutable) {
        this.root = root;
        this.mutable = mutable;
    }

    /**
     * Create a new route model instance for storing navigation targets mapped
     * by url templates.
     */
    static RouteModel create(boolean mutable) {
        return new RouteModel(mutable);
    }

    /**
     * Copy the given model into a new one.
     *
     * @param original
     *            the original model.
     * @param mutable
     *            mutable state of the result model.
     * @return a copy of the original model.
     */
    static RouteModel copy(RouteModel original, boolean mutable) {
        return new RouteModel(new RouteSegment(original.root), mutable);
    }

    /**
     * Collects all routes mapping the url template with the
     * {@link RouteTarget}.
     *
     * @return a {@link Map} containing all url templates and their specific
     *         targets.
     */
    Map<String, RouteTarget> getRoutes() {
        return root.getRoutes();
    }

    /**
     * Gets whether this model is empty and no routes are registered.
     *
     * @return true if this model is empty, otherwise false.
     */
    boolean isEmpty() {
        return root.isEmpty();
    }

    /**
     * Remove a path by its url template.
     *
     * @param urlTemplate
     *            the full url template.
     */
    void removeRoute(String urlTemplate) {
        throwIfImmutable();
        root.removeSubRoute(urlTemplate);
    }

    /**
     * Add a urlTemplate template following this route segment. If the template
     * already exists an exception is thrown.
     *
     * @param urlTemplate
     *            a url template where parameters are defined by their ids and
     *            details.
     * @param target
     *            target to set for the given url template.
     * @throws InvalidRouteConfigurationException
     *             if the combination of urlTemplate and target doesn't make
     *             sense within the current state of the model.
     * @throws IllegalArgumentException
     *             in case the varargs are specified in the middle of the
     *             urlTemplate. Varargs can be specified only as the last
     *             segment definition.
     */
    void addRoute(String urlTemplate, RouteTarget target) {
        throwIfImmutable();
        root.addSubRoute(urlTemplate, target);
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
        return root.getNavigationRouteTarget(url);
    }

    /**
     * Finds a route target for the given urlTemplate and parameters.
     *
     * @param urlTemplate
     *            the full url template.
     * @param parameters
     *            the parameters to use or null if no parameters specified.
     * @return a route result containing the target and parameter values mapped
     *         by their ids.
     * @throws IllegalArgumentException
     *             in case urlTemplate is not registered or the parameters do
     *             not match with the template.
     */
    RouteTarget getRouteTarget(String urlTemplate, RouteParameters parameters) {
        AtomicReference<RouteTarget> target = new AtomicReference<>();
        root.matchSegmentTemplatesWithParameters(urlTemplate, parameters, null,
                routeSegment -> target.set(routeSegment.getTarget()));
        return target.get();
    }

    /**
     * Gets a url path by replacing into the url template the route parameters.
     * <p>
     * In case all parameters defined in the urlTemplate are optional or
     * varargs, parameter arguments can be null and the path will be provided
     * without any parameters.
     * <p>
     * In case not all values found in <code>parameters</code> are used to
     * generate the final url, an <code>IllegalArgumentException</code>
     * exception is raised. In this case, consider providing the
     * <code>urlTemplate</code> containing the extra parameters found in
     * <code>parameters</code>.
     *
     * @param urlTemplate
     *            the full url template.
     * @param parameters
     *            the parameters to use.
     * @return the generated url.
     * @throws IllegalArgumentException
     *             in case urlTemplate is not registered or the parameters do
     *             not match exactly with the template.
     */
    String getUrl(String urlTemplate, RouteParameters parameters) {
        final List<String> result = new ArrayList<>();

        root.matchSegmentTemplatesWithParameters(urlTemplate, parameters,
                routeSegmentValue -> routeSegmentValue.value
                        .ifPresent(result::add),
                null);

        if (result.isEmpty()) {
            return "";
        } else {
            return String.join("/", result);
        }
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
            Set<RouteParameterFormatOption> format) {

        if (format.contains(RouteParameterFormatOption.NAME)
                && format.contains(RouteParameterFormatOption.MODIFIER)
                && format.contains(RouteParameterFormatOption.REGEX)) {
            return urlTemplate;
        }

        return root.formatUrlTemplate(urlTemplate,
                segment -> RouteFormat.formatSegment(segment, format));
    }

    /**
     * Gets the parameters found in the given urlTemplate. The result contains a
     * mapping between the name of the parameters and {@link RouteParameterData}
     * instances as values.
     *
     * @param urlTemplate
     *            the url template.
     * @return a {@link Map} containing the names of the parameters mapped by
     *         their formatted template using the given format.
     * @throws IllegalArgumentException
     *             in case urlTemplate is not registered or the parameters do
     *             not match with the template.
     */
    Map<String, RouteParameterData> getParameters(String urlTemplate) {
        Map<String, RouteParameterData> result = new HashMap<>();

        this.root.matchSegmentTemplates(urlTemplate, segment -> {
            if (segment.isParameter()) {
                result.put(segment.getName(), new RouteParameterData(
                        segment.getTemplate(), segment.getRegex()));
            }
        }, null);
        return result;
    }

    private void throwIfImmutable() {
        if (!mutable) {
            throw new IllegalStateException(
                    "Tried to mutate immutable model.");
        }
    }

}
