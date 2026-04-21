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

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

/**
 * A single point in the world, as reported by the browser.
 * <p>
 * Latitude, longitude and accuracy are always present. The remaining fields are
 * boxed {@link Double} because the device may not measure them: a laptop
 * without GPS typically reports {@code null} for altitude, heading and speed,
 * while a mobile phone with GPS typically reports all of them. Applications
 * should either tolerate {@code null} or check before using.
 *
 * @param latitude
 *            latitude in decimal degrees, positive north, negative south.
 *            Always present. Example: {@code 60.1699} for Helsinki
 * @param longitude
 *            longitude in decimal degrees, positive east, negative west. Always
 *            present. Example: {@code 24.9384} for Helsinki
 * @param accuracy
 *            the 1-sigma horizontal accuracy of the reading in metres: the true
 *            location lies within this distance of the reported
 *            {@code latitude}/{@code longitude} with ~68% probability. Smaller
 *            is better. Always present
 * @param altitude
 *            height in metres above the WGS 84 ellipsoid (approximately mean
 *            sea level), or {@code null} when the device cannot measure it
 * @param altitudeAccuracy
 *            the 1-sigma vertical accuracy of {@code altitude} in metres, or
 *            {@code null} when {@code altitude} is not available
 * @param heading
 *            the direction of travel in degrees clockwise from true north (0 =
 *            north, 90 = east, 180 = south, 270 = west), or {@code null} when
 *            the device cannot measure it or the user is stationary. Not
 *            meaningful when {@code speed} is 0
 * @param speed
 *            ground speed in metres per second, or {@code null} when the device
 *            cannot measure it
 */
public record GeolocationCoordinates(double latitude, double longitude,
        double accuracy, @Nullable Double altitude,
        @Nullable Double altitudeAccuracy, @Nullable Double heading,
        @Nullable Double speed) implements Serializable {
}
