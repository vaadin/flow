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
 * Server-side access to the browser's Screen Wake Lock API, which keeps the
 * device screen from dimming or locking — useful for dashboards, kiosks,
 * presentations, and recipe or workout screens.
 * <p>
 * {@link com.vaadin.flow.component.wakelock.WakeLock#request()} asks the
 * browser to hold a wake lock and re-acquire it across tab visibility changes;
 * {@link com.vaadin.flow.component.wakelock.WakeLock#release()} drops it.
 * {@link com.vaadin.flow.component.wakelock.WakeLock#activeSignal()} reflects
 * whether a lock is currently held, and
 * {@link com.vaadin.flow.component.wakelock.WakeLock#availabilitySignal()}
 * hints at whether the API is usable so you can hide wake-lock controls up
 * front.
 * <p>
 * The API requires a secure context (HTTPS or {@code localhost}). The browser
 * releases the lock automatically when the tab is hidden (the client
 * re-acquires it when the tab is shown again) and may drop it under power
 * saving or low battery.
 */
@NullMarked
package com.vaadin.flow.component.wakelock;

import org.jspecify.annotations.NullMarked;
