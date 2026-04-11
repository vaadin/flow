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
 * The current state of a {@link Geolocation#track tracking} session, as held by
 * {@link Geolocation#state()}.
 * <p>
 * A {@code GeolocationState} is always exactly one of three things:
 * <ul>
 * <li>{@link Pending} — the initial state, before the browser has produced any
 * answer</li>
 * <li>{@link GeolocationPosition} — the most recent successful reading. Updated
 * on every new reading while tracking is active</li>
 * <li>{@link GeolocationError} — the browser's most recent error (permission
 * denied, position unavailable, or timeout)</li>
 * </ul>
 * <p>
 * The sealed interface is designed for exhaustive pattern matching. A
 * {@code switch} on a {@code GeolocationState} that covers all three permitted
 * subtypes is guaranteed complete at compile time — adding a new state in a
 * future version of Flow would break existing switches so that callers are
 * forced to decide how to handle it.
 *
 * <pre>
 * switch (geo.state().get()) {
 * case GeolocationState.Pending p -&gt; {
 *     // waiting for the first fix
 * }
 * case GeolocationPosition pos -&gt; map.setCenter(pos.coords());
 * case GeolocationError err -&gt; showError(err.message());
 * }
 * </pre>
 */
public sealed interface GeolocationState extends Serializable permits
        GeolocationState.Pending, GeolocationPosition, GeolocationError {

    /**
     * The initial state of a newly started tracking session, used until the
     * browser reports the first position (or the first error).
     */
    record Pending() implements GeolocationState {
    }
}
