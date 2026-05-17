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
package com.vaadin.flow.component.trigger;

import java.util.Objects;

/**
 * Base class for actions whose client-side outcome is reported back to the
 * server with a typed payload.
 * <p>
 * The action declares the payload type at construction; when the client calls
 * its server-notification callback the framework deserialises the JSON argument
 * into a {@code T} via Jackson and invokes
 * {@link #applyServerSideEffect(Object)} on the UI thread.
 * <p>
 * Example:
 *
 * <pre>{@code
 * public record ClipboardResult(boolean success, String error) { }
 *
 * public class ClipboardCopyAction
 *         extends AbstractCallbackAction<ClipboardResult> {
 *     public ClipboardCopyAction(Argument<String> text) {
 *         super("flow:clipboard-copy", ClipboardResult.class);
 *         ...
 *     }
 *
 *     &#64;Override
 *     public void applyServerSideEffect(ClipboardResult result) {
 *         ...
 *     }
 * }
 * }</pre>
 *
 * Client-side, the matching action factory calls its {@code notifyServer}
 * callback with a single value matching {@code T}'s JSON shape (record → keyed
 * object, list → array, primitive → primitive).
 *
 * @param <T>
 *            the payload type the client reports back
 */
public abstract class AbstractCallbackAction<T> extends AbstractAction {

    private final Class<T> payloadType;

    /**
     * Creates a callback action.
     *
     * @param typeId
     *            namespaced type id matching a client factory, not {@code null}
     * @param payloadType
     *            the payload type the client reports back, not {@code null}
     */
    protected AbstractCallbackAction(String typeId, Class<T> payloadType) {
        super(typeId);
        this.payloadType = Objects.requireNonNull(payloadType);
    }

    /**
     * The payload type this action's server callback receives.
     *
     * @return the payload type, never {@code null}
     */
    public final Class<T> getPayloadType() {
        return payloadType;
    }

    /**
     * Called on the UI thread when the client posts a notification back via the
     * per-host return channel after this action ran. The framework deserialises
     * the JSON payload into a {@code T} before calling this method.
     *
     * @param payload
     *            the deserialised payload; may be {@code null} if the client
     *            invoked the callback with no argument
     */
    public abstract void applyServerSideEffect(T payload);
}
