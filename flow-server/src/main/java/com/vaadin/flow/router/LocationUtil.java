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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.internal.UrlUtil;

/**
 * Utility class exposing reusable utility methods for location.
 *
 * @since 2.7
 */
public class LocationUtil {

    // Prevent instantiation of util class
    private LocationUtil() {
    }

    /**
     * Throws {@link InvalidLocationException} if the provided path is not
     * parseable as a relative path. A relative path should be parseable as a
     * URI without a scheme or host, it should not contain any <code>..</code>
     * segments and it shouldn't start with <code>/</code>.
     *
     * @param path
     *            the (decoded) path to check, not null
     */
    public static void verifyRelativePath(String path) {
        assert path != null;

        try {
            // Ignore forbidden chars supported in route definitions
            String strippedPath = path.replaceAll("[{}*]", "");

            // : is completely valid in a path but URI will think that defines a
            // protocol so skip it for the check
            URI uri = new URI(UrlUtil.encodeURI(strippedPath).replace(":", ""));
            if (uri.isAbsolute()) {
                // "A URI is absolute if, and only if, it has a scheme
                // component"
                throw new InvalidLocationException(
                        "Relative path cannot contain an URI scheme");
            } else if (uri.getPath().startsWith("/")) {
                throw new InvalidLocationException(
                        "Relative path cannot start with /");
            } else if (hasIncorrectParentSegments(uri.getRawPath())) {
                throw new InvalidLocationException(
                        "Relative path cannot contain .. segments");
            }
        } catch (URISyntaxException e) {
            throw new InvalidLocationException("Cannot parse path: " + path, e);
        }

        // All is OK if we get here
    }

    /**
     * Parses the given path to parts split by the path separator.
     * <p>
     * Ignores the query string and fragment if either is present and
     * removeExtraParts is true. The path is verified with
     * {@link #verifyRelativePath(String)}.
     *
     * @param path
     *            the path to parse
     * @param removeExtraParts
     *            true to remove a potential query string and a URI fragment,
     *            false to use the path as is
     * @return tha path split into parts
     */
    public static List<String> parsePathToSegments(String path,
            boolean removeExtraParts) {
        final String basePath;
        int endIndex = path.indexOf(Location.QUERY_SEPARATOR);
        if (removeExtraParts && endIndex >= 0) {
            basePath = path.substring(0, endIndex);
        } else if (removeExtraParts && path.contains("#")) {
            basePath = path.substring(0, path.indexOf('#'));
        } else {
            basePath = path;
        }

        verifyRelativePath(basePath);

        List<String> splitList = Arrays
                .asList(basePath.split(Location.PATH_SEPARATOR));
        if (basePath.endsWith(Location.PATH_SEPARATOR)) {
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
     * Handles given location when it is either {@code null} or starts with "/".
     *
     * @param location
     *            the location to handle
     * @return the cleaned up location, not {@code null}
     */
    public static String ensureRelativeNonNull(String location) {
        if (location == null) {
            return "";
        }
        if (location.startsWith("/")) {
            location = location.substring(1);
        }
        return location.trim();
    }

    /**
     * Parses query parameters from the given location.
     *
     * @param location
     *            the location to parse the query parameters from
     * @return the query parameters
     */
    public static QueryParameters parseQueryParameters(String location) {
        if (location == null) {
            return QueryParameters.empty();
        }

        int beginIndex = location.indexOf(Location.QUERY_SEPARATOR);
        if (beginIndex < 0) {
            return QueryParameters.empty();
        }
        String query;

        // URI::getRawQuery as decoding of parameters is done in
        // QueryParameters. For issue with URI::getQuery, see:
        // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8214423
        try {
            query = new java.net.URI(location).getRawQuery();
        } catch (URISyntaxException ignore) { // NOSONAR
            query = null;
        }
        if (query == null) {
            query = location.substring(beginIndex + 1);
        }

        return QueryParameters.fromString(query);
    }

    private static boolean hasIncorrectParentSegments(String path) {
        // the actual part that we do not support is '../' so this
        // shouldn't catch 'el..ement' nor '..element'
        if (path.startsWith("../")) {
            return true;
        }
        if (path.contains("/../")) {
            return true;
        }
        if (path.endsWith("/..")) {
            return true;
        }
        if (path.equals("..")) {
            return true;
        }
        return false;
    }
}
