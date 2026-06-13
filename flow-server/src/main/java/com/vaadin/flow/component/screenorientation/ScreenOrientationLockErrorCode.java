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
package com.vaadin.flow.component.screenorientation;

/**
 * Typed reasons why a screen-orientation lock request can fail.
 * <p>
 * Returned by {@link ScreenOrientationLockError#errorCode()}. The client maps
 * the browser failure to one of these constants before reporting it to the
 * server, so application code can branch with an exhaustive {@code switch}
 * instead of inspecting browser-specific error strings.
 */
public enum ScreenOrientationLockErrorCode {
    /**
     * The browser does not implement the Screen Orientation API, or does not
     * allow locking on this device (for example a desktop that has no physical
     * orientation to lock to).
     */
    NOT_SUPPORTED,

    /**
     * The document is not in fullscreen, which most browsers require before
     * honoring a lock request.
     */
    SECURITY,

    /**
     * A newer lock or unlock call superseded this request before the browser
     * resolved it.
     */
    ABORT,

    /**
     * The browser rejected the request for a reason that does not match a more
     * specific code, or the executeJs round-trip itself failed.
     * {@link ScreenOrientationLockError#debugInfo()} holds the underlying
     * browser message for diagnostics.
     */
    UNKNOWN;
}
