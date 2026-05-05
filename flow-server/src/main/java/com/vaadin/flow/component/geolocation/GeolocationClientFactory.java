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

import org.jspecify.annotations.NullMarked;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Lookup;

/**
 * Factory SPI that produces {@link GeolocationClient} instances per {@link UI}.
 * Resolved via {@link Lookup} when a {@link Geolocation} facade is constructed:
 * a single factory registered via Java's service loader (a
 * {@code META-INF/services/com.vaadin.flow.component.geolocation.GeolocationClientFactory}
 * file) replaces the built-in browser-backed client for every {@code UI} in the
 * application.
 * <p>
 * Use cases for registering a factory:
 * <ul>
 * <li>In-memory test drivers that drive {@code Geolocation} without a real
 * browser.</li>
 * <li>Native bridges in hybrid mobile/desktop shells (Cordova, Electron,
 * Capacitor) that report positions through a non-DOM channel.</li>
 * </ul>
 * Application code does not implement this interface directly — registering a
 * factory replaces the client for the whole application, so it is an
 * integration concern, not a per-call API.
 */
@NullMarked
public interface GeolocationClientFactory extends Serializable {

    /**
     * Creates a {@link GeolocationClient} for the given UI. Called once per UI,
     * the first time {@link UI#getGeolocation()} is invoked.
     *
     * @param ui
     *            the UI for which the client is created
     * @return a client that will receive every {@code Geolocation} call for
     *         that UI; never null
     */
    GeolocationClient create(UI ui);
}
