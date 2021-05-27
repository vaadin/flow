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
package com.vaadin.flow.router;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Represents a relative URL made up of path segments and query parameters, but
 * lacking e.g. the hostname that can also be present in URLs.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Location implements Serializable {
    private static final String PATH_SEPARATOR = "/";
    private static final String QUERY_SEPARATOR = "?";

    private final List<String> segments;
    private final QueryParameters queryParameters;

    /**
     * Creates a new {@link Location} object for given location string.
     * <p>
     * This string can contain relative path and query parameters, if needed.
     * <p>
     * A possible "/" prefix of the location is ignored and a <code>null</code>
     * location is interpreted as <code>""</code>
     *
     * @param location
     *            the relative location or <code>null</code> which is
     *            interpreted as <code>""</code>
     */
    public Location(String location) {
        this(parsePath(ensureRelativeNonNull(location), true),
                parseParams(ensureRelativeNonNull(location)));
    }

    private static String ensureRelativeNonNull(String location) {
        if (location == null) {
            return "";
        }
        if (location.startsWith("/")) {
            location = location.substring(1);
        }
        return location.trim();
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
     * @throws IllegalArgumentException
     *             if location string contains query parameters inside
     */
    public Location(String location, QueryParameters queryParameters) {
        this(parsePath(ensureRelativeNonNull(location), false),
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
     * Gets the path string with {@link QueryParameters}.
     *
     * @return path string with parameters
     */
    public String getPathWithQueryParameters() {
        String basePath = getPath();
        assert !basePath.contains(
                QUERY_SEPARATOR) : "Base path can not contain query separator="
                        + QUERY_SEPARATOR;

        String params = queryParameters.getQueryString();
        if (params.isEmpty()) {
            return basePath;
        } else {
            return basePath + QUERY_SEPARATOR + params;
        }
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

    private static QueryParameters parseParams(String path) {
        int beginIndex = path.indexOf(QUERY_SEPARATOR);
        if (beginIndex < 0) {
            return QueryParameters.empty();
        }
        String query;

        try {
            query = new java.net.URI(path).getQuery();
        } catch (URISyntaxException ignore) { // NOSONAR
            query = null;
        }

        if (query == null) {
            // decoding of parameters is done in QueryParameters
            query = path.substring(beginIndex + 1);
        }

        return QueryParameters.fromString(query);
    }

    private static List<String> parsePath(String path,
            boolean stripQueryString) {
        final String basePath;
        int endIndex = path.indexOf(QUERY_SEPARATOR);
        if (stripQueryString && endIndex >= 0) {
            basePath = path.substring(0, endIndex);
        } else {
            basePath = path;
        }

        verifyRelativePath(basePath);

        List<String> splitList = Arrays.asList(basePath.split(PATH_SEPARATOR));
        if (basePath.endsWith(PATH_SEPARATOR)) {
            // Explicitly add "" to the end even though it's ignored by
            // String.split
            List<String> result = new ArrayList<>(splitList.size() + 1);
            result.addAll(splitList);
            result.add("");
            return result;
        } else {
            return splitList;
        }
    }

    /**
     * Throws {@link IllegalArgumentException} if the provided path is not
     * relative. A relative path should be parseable as a URI without a scheme
     * or host, it should not contain any <code>..</code> segments and it
     * shouldn't start with <code>/</code>.
     *
     * @param path
     *            the (decoded) path to check, not null
     */
    private static void verifyRelativePath(String path) {
        assert path != null;

        try {
            // Ignore forbidden chars supported in route definitions
            String strippedPath = path.replaceAll("[{}*]", "");

            URI uri = new URI(URLEncoder.encode(strippedPath, UTF_8.name()));
            if (uri.isAbsolute()) {
                // "A URI is absolute if, and only if, it has a scheme
                // component"
                throw new IllegalArgumentException(
                        "Relative path cannot contain an URI scheme");
            } else if (uri.getPath().startsWith("/")) {
                throw new IllegalArgumentException(
                        "Relative path cannot start with /");
            } else if (hasIncorrectParentSegments(uri.getRawPath())) {
                throw new IllegalArgumentException(
                        "Relative path cannot contain .. segments");
            }
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Cannot parse path: " + path, e);
        }

        // All is OK if we get here
    }

    private static boolean hasIncorrectParentSegments(String path) {
        // the actual part that we do not support is '../' so this
        // shouldn't catch 'el..ement' nor '..element'
        if (path.startsWith("..%2F")) {
            return true;
        }
        if (path.contains("%2F..%2F")) {
            return true;
        }
        if (path.endsWith("%2F..")) {
            return true;
        }
        if (path.equals("..")) {
            return true;
        }
        return false;
    }
}
