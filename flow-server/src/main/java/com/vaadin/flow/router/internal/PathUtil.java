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
 *
 */
package com.vaadin.flow.router.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utility methods for accessing path segments.
 */
public class PathUtil {

    /**
     * Returns a unmodifiable list containing the segments of the specified
     * path.
     *
     * @param path
     *            url path to get splitted into segments. The path may also
     *            start with a slash `/` but it may not contain the url
     *            protocol.
     * @return a List containing the segments of the path.
     */
    public static List<String> getSegmentsList(String path) {
        path = trimPath(path);

        final String[] segments = path.split("/");
        if (segments.length == 1 && segments[0].isEmpty()) {
            // This happens on root.
            return Collections.emptyList();

        } else {
            return Arrays.asList(segments);
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
        return String.join("/", segments);
    }

    /**
     * Join the segments into a url path.
     *
     * @param path
     *            path prefix.
     * @param segments
     *            path segments.
     * @return path form from input segments.
     */
    public static String getPath(String path, List<String> segments) {
        path = trimPath(path);

        return (path.isEmpty() ? "" : path)
                + ((segments == null || segments.isEmpty()) ? ""
                        : ("/" + String.join("/", segments)));
    }

    private static String trimPath(String path) {
        path = path.trim();

        if (path.startsWith("/")) {
            path = path.substring(1, path.length());
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

}
