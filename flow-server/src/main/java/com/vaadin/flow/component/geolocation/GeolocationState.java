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

/**
 * Represents the state of a geolocation tracking request.
 * <p>
 * Three states are possible:
 * <ul>
 * <li>{@link Pending} — initial state before the browser responds</li>
 * <li>{@link GeolocationPosition} — a successful position fix</li>
 * <li>{@link GeolocationError} — the browser reported an error</li>
 * </ul>
 * <p>
 * The sealed type enables exhaustive pattern matching:
 *
 * <pre>
 * switch (geo.state().get()) {
 * case GeolocationState.Pending p -&gt; {
 * }
 * case GeolocationPosition pos -&gt; map.setCenter(pos.coords());
 * case GeolocationError err -&gt; showError(err.message());
 * }
 * </pre>
 */
public sealed interface GeolocationState extends Serializable permits
        GeolocationState.Pending, GeolocationPosition, GeolocationError {

    /**
     * Initial state before the browser has responded to the tracking request.
     */
    record Pending() implements GeolocationState {
    }
}
