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
 * or it reported an error.
 * <p>
 * Passed to {@link Geolocation#get} callbacks and held (possibly as
 * {@code null} before the first fix) by the signal exposed by
 * {@link GeolocationTracker#value()}.
 * <p>
 * The sealed interface is designed for exhaustive pattern matching. A
 * {@code switch} covering both permitted subtypes is guaranteed complete at
 * compile time — adding a new variant in a future version of Flow would break
 * existing switches so that callers are forced to decide how to handle it.
 *
 * <pre>
 * switch (result) {
 * case GeolocationPosition pos -&gt; map.setCenter(pos.coords());
 * case GeolocationError err -&gt; showError(err.message());
 * }
 * </pre>
 *
 * For a tracker, the signal starts as {@code null} until the first reading
 * arrives. Match with {@code case null} to handle the waiting state:
 *
 * <pre>
 * switch (tracker.value().get()) {
 * case null -&gt; showSpinner();
 * case GeolocationPosition pos -&gt; map.setCenter(pos.coords());
 * case GeolocationError err -&gt; showError(err.message());
 * }
 * </pre>
 */
public sealed interface GeolocationResult extends Serializable
        permits GeolocationPosition, GeolocationError {
}
