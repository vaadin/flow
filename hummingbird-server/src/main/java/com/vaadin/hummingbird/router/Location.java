/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.router;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents a relative URL made up of path segments, but lacking e.g. the
 * hostname and query string that can also be present in URLs.
 *
 * @since
 * @author Vaadin Ltd
 */
public class Location implements Serializable {

    private static final String PATH_SEPARATOR = "/";
    private List<String> segments;

    /**
     * Creates a new location for the given relative path.
     *
     * @param path
     *            the relative path, not <code>null</code>
     */
    public Location(String path) {
        this(parsePath(path));
    }

    /**
     * Creates a new location based on a list of path segments.
     *
     * @param segments
     *            a non-empty list of path segments, not <code>null</code>
     */
    public Location(List<String> segments) {
        if (segments == null) {
            throw new IllegalArgumentException("Segments cannot be null");
        }
        if (segments.isEmpty()) {
            throw new IllegalArgumentException(
                    "There must be at least one segment");
        }

        this.segments = segments;
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
     * @return an optional new location
     */
    public Optional<Location> getSubLocation() {
        List<String> subSegments = segments.subList(1, segments.size());
        if (subSegments.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(new Location(subSegments));
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

    private static List<String> parsePath(String path) {
        assert !path.startsWith(PATH_SEPARATOR) : "path should be relative";
        assert !path.contains("?") : "query string not yet supported";

        verifyRelativePath(path);

        List<String> splitList = Arrays.asList(path.split(PATH_SEPARATOR));
        if (path.endsWith(PATH_SEPARATOR)) {
            // Explicitly add "" to the end even though it's ignored by
            // String.split
            ArrayList<String> result = new ArrayList<>(splitList.size() + 1);
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
     *            the path to check, not null
     */
    public static void verifyRelativePath(String path) {
        assert path != null;

        try {
            // Ignore forbidden chars supported in route definitions
            String strippedPath = path.replaceAll("[{}*]", "");

            URI uri = new URI(strippedPath);
            if (uri.isAbsolute()) {
                // "A URI is absolute if, and only if, it has a scheme
                // component"
                throw new IllegalArgumentException(
                        "Relative path cannot contain an URI scheme");
            } else if (uri.getPath().startsWith("/")) {
                throw new IllegalArgumentException(
                        "Relative path cannot start with /");
            } else if (uri.getRawPath().contains("..")) {
                throw new IllegalArgumentException(
                        "Relative path cannot contain .. segments");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Cannot parse path: ", e);
        }

        // All is OK if we get here
    }
}
