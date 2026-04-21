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
 * The outcome of a geolocation request — either the browser produced a reading,
 * or it reported an error, or the tracker is still waiting for the first
 * answer.
 * <p>
 * Returned to {@link Geolocation#get} callbacks and held by the signal exposed
 * by {@link GeolocationTracker#value()}. A {@code GeolocationResult} is always
 * exactly one of three things:
 * <ul>
 * <li>{@link Pending} — the initial state of a newly started tracker, before
 * the browser has produced any answer. One-shot {@link Geolocation#get} never
 * resolves to this value.</li>
 * <li>{@link GeolocationPosition} — the most recent successful reading. While
 * tracking is active, the signal is updated on every new reading.</li>
 * <li>{@link GeolocationError} — the browser's most recent error (permission
 * denied, position unavailable, or timeout).</li>
 * </ul>
 * <p>
 * The sealed interface is designed for exhaustive pattern matching. A
 * {@code switch} covering all three permitted subtypes is guaranteed complete
 * at compile time — adding a new variant in a future version of Flow would
 * break existing switches so that callers are forced to decide how to handle
 * it.
 *
 * <pre>
 * switch (result) {
 * case GeolocationResult.Pending p -&gt; {
 *     // waiting for the first fix
 * }
 * case GeolocationPosition pos -&gt; map.setCenter(pos.coords());
 * case GeolocationError err -&gt; showError(err.message());
 * }
 * </pre>
 */
public sealed interface GeolocationResult extends Serializable permits
        GeolocationResult.Pending, GeolocationPosition, GeolocationError {

    /**
     * The initial state of a newly started tracking session, used until the
     * browser reports the first position (or the first error).
     */
    record Pending() implements GeolocationResult {
    }
}
