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
 * Anything a tracker can currently hold — a successful reading, an error, or
 * the initial "waiting for first reading" state.
 * <p>
 * Held by the signal exposed by {@link GeolocationTracker#valueSignal()}. A
 * {@code GeolocationResult} is always exactly one of three things:
 * <ul>
 * <li>{@link GeolocationPending} — the initial state of a newly started
 * tracker, before the browser has reported anything.</li>
 * <li>{@link GeolocationPosition} — a successful reading.</li>
 * <li>{@link GeolocationError} — the browser reported an error.</li>
 * </ul>
 * For the one-shot {@link Geolocation#get} callback use the narrower
 * {@link GeolocationOutcome}, which excludes {@link GeolocationPending}
 * (one-shot requests never produce that value).
 * <p>
 * The sealed hierarchy is designed for exhaustive pattern matching. A
 * {@code switch} covering the three permitted variants is guaranteed complete
 * at compile time.
 *
 * <pre>
 * switch (tracker.valueSignal().get()) {
 * case GeolocationPending p -&gt; showSpinner();
 * case GeolocationPosition pos -&gt; map.setCenter(pos.coords());
 * case GeolocationError err -&gt; showError(err.message());
 * }
 * </pre>
 */
public sealed interface GeolocationResult extends Serializable
        permits GeolocationOutcome, GeolocationPending {
}
