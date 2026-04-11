/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.component.geolocation;

/**
 * A successful location reading: the coordinates the browser reported and the
 * moment in time they were taken.
 * <p>
 * This is one of the three possible values of a {@link Geolocation#state()}
 * signal, and is the payload passed to the {@code onSuccess} callbacks of
 * {@link Geolocation#get}.
 *
 * @param coords
 *            the latitude/longitude and related fields; see
 *            {@link GeolocationCoordinates}
 * @param timestamp
 *            the moment the reading was taken, as milliseconds since the Unix
 *            epoch (1970-01-01T00:00:00Z). Convert to an
 *            {@link java.time.Instant} with
 *            {@code Instant.ofEpochMilli(timestamp)} when needed
 */
public record GeolocationPosition(GeolocationCoordinates coords,
        long timestamp) implements GeolocationState {
}
