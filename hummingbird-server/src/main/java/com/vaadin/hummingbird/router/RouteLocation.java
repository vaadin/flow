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

import java.util.Optional;

/**
 * Location with support for the syntax used in route definitions used with e.g.
 * {@link ModifiableRouterConfiguration#setRoute(String, Class, Class)}.
 *
 * @since
 * @author Vaadin Ltd
 */
public class RouteLocation extends Location {

    /**
     * Creates a new route location from a regular location.
     *
     * @param location
     *            the original location
     */
    public RouteLocation(Location location) {
        super(location.getSegments());
    }

    /**
     * Gets the sub location as a {@link RouteLocation}.
     *
     * @see #getSubLocation()
     *
     * @return an optional new route location, or an empty optional if this
     *         route location has only one path segment
     */
    public Optional<RouteLocation> getRouteSubLocation() {
        return getSubLocation().map(RouteLocation::new);
    }

    /**
     * Checks whether the first segment is a placeholder segment. Also checks
     * for illegal use of the placeholder segment identifiers if the segment is
     * not a placeholder.
     *
     * @return <code>true</code> if the first segment is a placeholder segment,
     *         <code>false</code> otherwise.
     */
    public boolean startsWithPlaceholder() {
        String firstSegment = getFirstSegment();

        boolean isPlaceholder = firstSegment.startsWith("{")
                && firstSegment.endsWith("}");
        if (!isPlaceholder
                && (firstSegment.contains("{") || firstSegment.contains("}"))) {
            throw new IllegalStateException(
                    "{ and } are only allowed in the start and end of a segment");
        }
        return isPlaceholder;
    }

    /**
     * Checks whether a the first segment of this location is a wildcard
     * segment. Also checks for illegal use of the wildcard segment identifier
     * if the segment is not a wildcard.
     *
     * @return <code>true</code> if the first segment is a wildcard segment,
     *         <code>false</code> otherwise.
     */
    public boolean startsWithWildcard() {
        String firstSegment = getFirstSegment();

        boolean isWildcard = "*".equals(firstSegment);
        if (!isWildcard && firstSegment.contains("*")) {
            throw new IllegalStateException("* is only valid as \"/*\"");
        }
        return isWildcard;
    }

    /**
     * Gets the name of the placeholder in the first segment of this location.
     * The placeholder name is the part of the segment that is enclosed in
     * <code>{</code> and <code>}</code>. This method should only be used if
     * {@link #startsWithPlaceholder()} returns <code>true</code>.
     *
     * @return the name of the placeholder, not null
     */
    public String getPlaceholderName() {
        if (!startsWithPlaceholder()) {
            throw new IllegalStateException(
                    "Can only get a placeholder name if for a location that starts with a placeholder segment.");
        }

        String segment = getFirstSegment();

        // Catch problems if/when startsWithPlaceholder is changed to also
        // accept e.g. "prefix{name}"
        assert segment.startsWith("{") && segment.endsWith("}");
        return segment.substring(1, segment.length() - 1);
    }
}
