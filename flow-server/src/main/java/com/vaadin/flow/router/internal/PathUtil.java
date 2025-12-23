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
package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.vaadin.flow.internal.UrlUtil;

/**
 * Utility class which contains various methods for parsing a route url into
 * segments.
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class PathUtil implements Serializable {

    /**
     * Returns an unmodifiable list containing the segments of the specified
     * path.
     *
     * @param path
     *            url path to split into segments. The path may also start with
     *            a slash `/` but it may not contain the url protocol.
     * @return a List containing the segments of the path.
     */
    public static List<String> getSegmentsList(String path) {
        path = path == null ? "" : trimSegmentsString(path);

        final String[] segments = path.split("/");
        if (segments.length == 1 && segments[0].isEmpty()) {
            // This happens on root.
            return Collections.emptyList();

        } else {
            return Collections.unmodifiableList(Arrays.asList(segments));
        }
    }

    /**
     * Returns an unmodifiable list containing the URL-decoded segments of the
     * specified path. Each segment is decoded using
     * {@link UrlUtil#decodeURIComponent(String)}, which properly handles
     * percent-encoded characters including slashes (%2F), spaces (%20), and
     * other special characters.
     * <p>
     * This method is designed for processing paths where individual segments
     * may contain URL-encoded data that should be preserved after decoding.
     * For example, a path segment containing {@code %2F} will be decoded to
     * {@code /}, but this slash will not be treated as a path separator.
     *
     * @param path
     *            url path to split into segments and decode. The path may also
     *            start with a slash `/` but it may not contain the url
     *            protocol.
     * @return a List containing the decoded segments of the path.
     */
    public static List<String> getSegmentsListWithDecoding(String path) {
        path = path == null ? "" : trimSegmentsString(path);

        final String[] segments = path.split("/");
        if (segments.length == 1 && segments[0].isEmpty()) {
            // This happens on root.
            return Collections.emptyList();

        } else {
            return Arrays.stream(segments)
                    .map(UrlUtil::decodeURIComponent)
                    .collect(Collectors.toUnmodifiableList());
        }
    }

    /**
     * Join the segments into a url path.
     *
     * @param segments
     *            path segments.
     * @return path form from input segments.
     */
    public static String getPath(List<String> segments) {
        return trimSegmentsString(
                segments == null ? "" : String.join("/", segments));
    }

    /**
     * Join the segments into a url path.
     *
     * @param basePath
     *            path prefix.
     * @param segments
     *            path segments following the prefix.
     * @return the path form by concatenating basePath and segments.
     */
    public static String getPath(String basePath, List<String> segments) {
        basePath = trimPath(basePath);

        return trimPath(
                basePath + ((segments == null || segments.isEmpty()) ? ""
                        : ("/" + String.join("/", segments))));
    }

    /**
     * Trim the path by removing any leading and trailing whitespaces and
     * slashes.
     *
     * @param path
     *            url path to trim.
     * @return a String representing the input path without any leading and
     *         trailing whitespaces and slashes.
     */
    public static String trimPath(String path) {
        if (path == null) {
            return "";
        }

        path = path.trim();

        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * Trim the path by removing any leading and trailing whitespaces and
     * trailing slashes.
     *
     * @param path
     *            url path to trim, not null
     * @return a String representing the input path without any leading and
     *         trailing whitespaces or trailing slash.
     */
    public static String trimSegmentsString(String path) {
        Objects.requireNonNull(path);

        path = path.trim();

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

}
