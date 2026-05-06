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
 * The value held by {@link GeolocationWatcher#positionSignal()} — a successful
 * reading, an error, or the initial "waiting for first reading" state. Always
 * exactly one of:
 * <ul>
 * <li>{@link GeolocationPending} — initial state, before the browser has
 * reported anything.</li>
 * <li>{@link GeolocationPosition} — a successful reading.</li>
 * <li>{@link GeolocationError} — the browser reported an error.</li>
 * </ul>
 * The sealed hierarchy supports exhaustive pattern matching:
 *
 * <pre>
 * switch (watcher.positionSignal().get()) {
 * case GeolocationPending p -&gt; showSpinner();
 * case GeolocationPosition pos -&gt; map.setCenter(pos.coords());
 * case GeolocationError err -&gt; showError(err.message());
 * }
 * </pre>
 */
public sealed interface GeolocationResult extends Serializable
        permits GeolocationOutcome, GeolocationPending {
}
