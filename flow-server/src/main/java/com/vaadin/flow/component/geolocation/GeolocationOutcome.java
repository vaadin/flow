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
 * The actual answer to a geolocation request — either a successful reading or
 * an error. Narrower than {@link GeolocationResult}: the "waiting for first
 * reading" {@link GeolocationPending} state is excluded because one-shot
 * {@link Geolocation#get} never produces it.
 * <p>
 * Returned to the callback of {@link Geolocation#get}. Use this instead of
 * {@link GeolocationResult} when you only need to handle the Position / Error
 * branches and want the {@code switch} to stay exhaustive without a dead
 * Pending arm.
 *
 * <pre>
 * ui.getGeolocation().get(outcome -&gt; {
 *     switch (outcome) {
 *     case GeolocationPosition pos -&gt; showNearest(pos);
 *     case GeolocationError err -&gt; showManualEntry();
 *     }
 * });
 * </pre>
 */
public sealed interface GeolocationOutcome extends GeolocationResult
        permits GeolocationPosition, GeolocationError {
}
