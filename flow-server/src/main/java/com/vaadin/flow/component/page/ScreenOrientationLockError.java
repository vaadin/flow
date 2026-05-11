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
package com.vaadin.flow.component.page;

import java.io.Serializable;

/**
 * Describes a failed screen-orientation lock request.
 * <p>
 * Fields mirror the {@code DOMException} the browser rejects
 * {@code screen.orientation.lock()} with. The most common values for
 * {@code name} are:
 * <ul>
 * <li>{@code "NotSupportedError"} — the browser does not implement the Screen
 * Orientation API at all, or does not allow locking on this device.</li>
 * <li>{@code "SecurityError"} — the document is not in fullscreen, which most
 * browsers require for locking.</li>
 * <li>{@code "AbortError"} — a newer lock or unlock call superseded this
 * one.</li>
 * </ul>
 *
 * @param name
 *            the {@code DOMException} name, e.g. {@code "SecurityError"}
 * @param message
 *            the {@code DOMException} message, suitable for logging
 */
public record ScreenOrientationLockError(String name,
        String message) implements Serializable {
}
