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
 * Represents geographic coordinates as returned by the browser's Geolocation
 * API.
 *
 * @param latitude
 *            the latitude in decimal degrees
 * @param longitude
 *            the longitude in decimal degrees
 * @param accuracy
 *            the accuracy of the position in meters
 * @param altitude
 *            the altitude in meters above the WGS84 ellipsoid, or {@code null}
 *            if not available
 * @param altitudeAccuracy
 *            the accuracy of the altitude in meters, or {@code null} if not
 *            available
 * @param heading
 *            the direction of travel in degrees (0-360), or {@code null} if not
 *            available
 * @param speed
 *            the speed in meters per second, or {@code null} if not available
 */
public record GeolocationCoordinates(double latitude, double longitude,
        double accuracy, Double altitude, Double altitudeAccuracy,
        Double heading, Double speed) {
}
