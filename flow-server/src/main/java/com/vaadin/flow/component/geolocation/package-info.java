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
/**
 * Server-side access to the browser's Geolocation API for reading the user's
 * location.
 * <p>
 * {@link com.vaadin.flow.component.geolocation.Geolocation#getPosition(com.vaadin.flow.function.SerializableConsumer, com.vaadin.flow.function.SerializableConsumer)
 * Geolocation.getPosition(...)} reads the location once;
 * {@link com.vaadin.flow.component.geolocation.Geolocation#watchPosition(com.vaadin.flow.component.Component)
 * Geolocation.watchPosition(...)} returns a
 * {@link com.vaadin.flow.component.geolocation.GeolocationWatcher} that
 * delivers continuous updates (as a listener or a reactive signal) and stops
 * automatically when its owning component is detached.
 * {@link com.vaadin.flow.component.geolocation.Geolocation#availabilityHintSignal()}
 * gives a best-effort hint about whether permission has been granted, so you
 * can decide whether to prompt the user.
 * <p>
 * Geolocation requires a secure context (HTTPS or {@code localhost}) and
 * explicit user permission; the first request shows the browser's permission
 * prompt, and a denied or unavailable request reports a
 * {@link com.vaadin.flow.component.geolocation.GeolocationError}. Accuracy
 * varies by device, and fields such as altitude and speed may be absent when
 * the device cannot measure them.
 */
@NullMarked
package com.vaadin.flow.component.geolocation;

import org.jspecify.annotations.NullMarked;
