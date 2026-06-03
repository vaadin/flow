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
package com.vaadin.flow.component.wakelock;

import java.io.Serializable;
import java.util.Objects;

/**
 * Failure outcome reported to the consumer passed to
 * {@link WakeLock#request(com.vaadin.flow.function.SerializableConsumer)}.
 *
 * @param code
 *            machine-readable reason; never {@code null}
 * @param message
 *            human-readable detail suitable for diagnostics; never {@code null}
 *            but may be empty
 */
public record WakeLockError(WakeLockErrorCode code,
        String message) implements Serializable {

    public WakeLockError {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
