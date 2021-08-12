/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

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

            URI uri = new URI(URLEncoder.encode(strippedPath, UTF_8.name()));
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
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new InvalidLocationException("Cannot parse path: " + path, e);
        }

        // All is OK if we get here
    }

    /**
     * Parses the given path to parts split by the path separator, ignoring the
     * query string if present. The path is verified with
     * {@link #verifyRelativePath(String)}.
     * 
     * @param path
     *            the path to parse
     * @return tha path split into parts
     */
    public static List<String> parsePathToSegments(String path) {
        final String basePath;
        int endIndex = path.indexOf(Location.QUERY_SEPARATOR);
        if (endIndex >= 0) {
            basePath = path.substring(0, endIndex);
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
     * Parses query parameters from the given path.
     * 
     * @param path
     *            the path to parse the query parameters from
     * @return the query parameters
     */
    public static QueryParameters parseQueryParameters(String path) {
        int beginIndex = path.indexOf(Location.QUERY_SEPARATOR);
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
