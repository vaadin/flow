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
 * <b>Framework internal.</b> Factory SPI that produces
 * {@link GeolocationClient} instances per {@link UI}, resolved via
 * {@link Lookup} when a {@link Geolocation} facade is constructed. When a
 * factory is registered the resulting client replaces the built-in
 * browser-backed client for every {@code UI} in the application; when none is,
 * {@code Geolocation} uses the browser-backed client.
 * <p>
 * Used by external browserless test drivers to swap the production wire client
 * for an in-memory driver in environments where package-private cross-JAR
 * access is unreliable (split-classloader topologies such as Quarkus).
 * Application code does not reference this interface directly. May be renamed
 * or removed in a future release.
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
