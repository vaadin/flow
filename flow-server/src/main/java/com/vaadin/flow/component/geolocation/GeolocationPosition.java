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
 * Represents a geographic position as returned by the browser's Geolocation
 * API, consisting of coordinates and a timestamp.
 *
 * @param coords
 *            the geographic coordinates
 * @param timestamp
 *            the time at which the position was determined, in milliseconds
 *            since the Unix epoch
 */
public record GeolocationPosition(GeolocationCoordinates coords,
        long timestamp) implements GeolocationState {
}
