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
 * Server-side access to the browser's Screen Orientation API for observing and
 * locking the device's screen orientation.
 * <p>
 * {@link com.vaadin.flow.component.screenorientation.ScreenOrientation#orientationSignal()}
 * reports the current orientation (portrait, landscape, and its angle) and
 * updates as the device rotates.
 * {@link com.vaadin.flow.component.screenorientation.ScreenOrientation#lock(com.vaadin.flow.component.screenorientation.ScreenOrientationType)
 * ScreenOrientation.lock(...)} pins the screen to a chosen orientation.
 * <p>
 * Locking is mainly relevant on mobile devices and typically only succeeds
 * while the page is in fullscreen; it is commonly rejected on desktop browsers
 * and on devices without an orientation sensor. A failed lock reports a
 * {@link com.vaadin.flow.component.screenorientation.ScreenOrientationLockError}.
 */
@NullMarked
package com.vaadin.flow.component.screenorientation;

import org.jspecify.annotations.NullMarked;
