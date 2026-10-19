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
 * Internal narrowing of {@link GeolocationResult} to the two states that the
 * client port resolves to: {@link GeolocationPosition} or
 * {@link GeolocationError}. Excludes the "waiting for first reading"
 * {@link GeolocationPending} state, which is only meaningful for an active
 * {@link GeolocationWatcher} signal.
 * <p>
 * Package-private; the public API exposes successes and errors as separate
 * consumers instead.
 */
sealed interface GeolocationOutcome extends GeolocationResult
        permits GeolocationPosition, GeolocationError {
}
