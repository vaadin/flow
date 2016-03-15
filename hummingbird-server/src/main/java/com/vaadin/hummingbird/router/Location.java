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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a relative URL made up of path segments, but lacking e.g. the
 * hostname and query string that can also be present in URLs.
 *
 * @since
 * @author Vaadin Ltd
 */
public class Location implements Serializable {

    private List<String> segments;

    /**
     * Creates a new location for the given path.
     *
     * @param path
     *            the relative path, not <code>null</code>
     */
    public Location(String path) {
        this(parsePath(path));

        assert !path.startsWith("/") : "path should be relative";
        assert !path.contains("?") : "query string not yet supported";
    }

    /**
     * Creates a new location based on a list of path segments.
     *
     * @param segments
     *            a list of path segments, not <code>null</code>
     */
    public Location(List<String> segments) {
        assert segments != null;

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
     * @return the first path segment, or <code>null</code> if this path has no
     *         segments
     */
    public String getFirstSegment() {
        if (!hasSegments()) {
            return null;
        } else {
            return segments.get(0);
        }
    }

    /**
     * Checks whether this location has any segments.
     *
     * @return <code>true</code> if at least one path segment is available,
     *         <code>false</code> if there are no path segments
     */
    public boolean hasSegments() {
        return !segments.isEmpty();
    }

    /**
     * Creates a new location without the first path segment. Throws
     * {@link IllegalStateException} if this location has no path segments.
     *
     * @return a new location, not <code>null</code>
     */
    public Location getSubLocation() {
        if (segments.isEmpty()) {
            throw new IllegalStateException("Location has no path segments");
        } else {
            return new Location(segments.subList(1, segments.size()));
        }
    }

    private static List<String> parsePath(String path) {
        List<String> splitList = Arrays.asList(path.split("/"));
        if (path.endsWith("/")) {
            // Explicitly add "" to the end even though it's ignored by
            // String.split
            ArrayList<String> result = new ArrayList<>(splitList);
            result.add("");
            return result;
        } else {
            return splitList;
        }
    }
}
