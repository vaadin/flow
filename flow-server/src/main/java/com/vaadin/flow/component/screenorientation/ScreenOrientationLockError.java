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

import java.io.Serializable;

/**
 * Describes a failed screen-orientation lock request.
 * <p>
 * The client maps the browser failure to a typed
 * {@link ScreenOrientationLockErrorCode} before reporting it, so application
 * code can branch on {@link #errorCode()} with an exhaustive {@code switch}.
 * {@link #debugInfo()} carries the free-form browser description of the failure
 * — useful for log lines and bug reports, but the wording is not standardised
 * across browsers and must not be shown to end users as-is.
 *
 * @param errorCode
 *            the typed reason for the failure, never {@code null}
 * @param debugInfo
 *            a free-form description of the failure as reported by the browser,
 *            suitable for logging; never {@code null} but possibly empty
 */
public record ScreenOrientationLockError(
        ScreenOrientationLockErrorCode errorCode,
        String debugInfo) implements Serializable {
}
