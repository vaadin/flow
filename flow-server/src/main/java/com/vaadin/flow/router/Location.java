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
package com.vaadin.flow.router;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents a relative URL made up of path segments and query parameters, but
 * lacking e.g. the hostname that can also be present in URLs.
 * <p>
 * For related utility methods, see {@link LocationUtil}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Location implements Serializable {
    static final String PATH_SEPARATOR = "/";
    static final String QUERY_SEPARATOR = "?";

    private final List<String> segments;
    private final QueryParameters queryParameters;
    private String fragment;

    /**
     * Creates a new {@link Location} object for given location string.
     * <p>
     * This string can contain relative path and query parameters, if needed. A
     * possible fragment {@code #fragment} is also retained.
     * <p>
     * A possible "/" prefix of the location is ignored and a <code>null</code>
     * location is interpreted as <code>""</code>
     *
     * @param location
     *            the relative location or <code>null</code> which is
     *            interpreted as <code>""</code>]
     * @throws InvalidLocationException
     *             If the given string cannot be used for the {@link Location}
     */
    public Location(String location) throws InvalidLocationException {
        this(LocationUtil.parsePathToSegments(
                LocationUtil.ensureRelativeNonNull(location), true),
                LocationUtil.parseQueryParameters(location));
        int fragmentIndex = location == null ? -1 : location.indexOf('#');
        if (fragmentIndex > -1) {
            fragment = location.substring(fragmentIndex);
        }
    }

    /**
     * Creates a new {@link Location} object for given location string and query
     * parameters.
     * <p>
     * The location string can not contain query parameters. To pass query
     * parameters, either specify them in {@link QueryParameters} in this
     * constructor, or use {@link Location#Location(String)}
     * <p>
     * A possible "/" prefix of the location is ignored and a <code>null</code>
     * location is interpreted as <code>""</code>
     *
     *
     * @param location
     *            the relative location or <code>null</code> which is
     *            interpreted as <code>""</code>
     * @param queryParameters
     *            query parameters information, not {@code null}
     * @throws InvalidLocationException
     *             If the given string cannot be used for the {@link Location}
     */
    public Location(String location, QueryParameters queryParameters)
            throws InvalidLocationException {
        this(LocationUtil.parsePathToSegments(
                LocationUtil.ensureRelativeNonNull(location), false),
                queryParameters);
    }

    /**
     * Creates a new location based on a list of path segments.
     *
     * @param segments
     *            a non-empty list of path segments, not <code>null</code>
     */
    public Location(List<String> segments) {
        this(segments, QueryParameters.empty());
    }

    /**
     * Creates a new location based on a list of path segments and query
     * parameters.
     *
     * @param segments
     *            a non-empty list of path segments, not {@code null} and not
     *            empty
     * @param queryParameters
     *            query parameters information, not {@code null}
     */
    public Location(List<String> segments, QueryParameters queryParameters) {
        if (segments == null) {
            throw new IllegalArgumentException("Segments cannot be null");
        }
        if (segments.isEmpty()) {
            throw new IllegalArgumentException(
                    "There must be at least one segment");
        }
        if (queryParameters == null) {
            throw new IllegalArgumentException(
                    "Query parameters cannot be null");
        }

        this.segments = segments;
        this.queryParameters = queryParameters;
    }

    /**
     * Gets all the path segments of this location.
     *
     * @return a list of path segments
     */
    public List<String> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    /**
     * Gets the request parameters used for current location.
     *
     * @return the request parameters
     */
    public QueryParameters getQueryParameters() {
        return queryParameters;
    }

    /**
     * Gets the first segment of this path.
     *
     * @return the first path segment, not <code>null</code>
     */
    public String getFirstSegment() {
        return segments.get(0);
    }

    /**
     * Creates a new location without the first path segment. The result is
     * empty if this location only consists of one segment.
     *
     * @return an optional new location, or an empty optional if this location
     *         has only one path segment
     */
    public Optional<Location> getSubLocation() {
        List<String> subSegments = segments.subList(1, segments.size());
        if (subSegments.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(new Location(subSegments, queryParameters));
        }
    }

    /**
     * Gets the path of this location as a string.
     *
     * @return the location string, not <code>null</code>
     */
    public String getPath() {
        return segments.stream().collect(Collectors.joining("/"));
    }

    /**
     * Gets the path string with {@link QueryParameters} and including the
     * possible fragment if one existed.
     *
     * @return path string with parameters
     */
    public String getPathWithQueryParameters() {
        String basePath = getPath();
        assert !basePath.contains(QUERY_SEPARATOR)
                : "Base path can not contain query separator="
                        + QUERY_SEPARATOR;
        assert !basePath.contains("#") : "Base path can not contain fragment #";

        final StringBuilder pathBuilder = new StringBuilder(basePath);
        String params = queryParameters.getQueryString();
        if (!params.isEmpty()) {
            pathBuilder.append(QUERY_SEPARATOR).append(params);
        }
        if (fragment != null) {
            pathBuilder.append(fragment);
        }
        return pathBuilder.toString();
    }

    /**
     * Removes or adds slash to the end of the location path. Creates new
     * {@link Location} instance instead of modifying the old one.
     *
     * @return new {@link Location} instance with updated path
     */
    public Location toggleTrailingSlash() {
        // Even Location for "" still contains one (empty) segment
        assert !segments.isEmpty();

        String lastSegment = segments.get(segments.size() - 1);

        if (segments.size() == 1 && "".equals(lastSegment)) {
            throw new IllegalArgumentException(
                    "Can't toggle ending slash for the \"\" location");
        }

        if (lastSegment.isEmpty()) {
            // New location without ending empty segment
            return new Location(segments.subList(0, segments.size() - 1),
                    queryParameters);
        } else {
            // Add empty ending segment
            List<String> newSegments = new ArrayList<>(segments);
            newSegments.add("");
            return new Location(newSegments, queryParameters);
        }
    }

}
