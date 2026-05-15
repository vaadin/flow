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
package com.vaadin.flow.component.trigger.internal;

import java.util.Objects;

import com.vaadin.flow.component.trigger.AbstractAction;
import com.vaadin.flow.function.SerializableRunnable;

/**
 * Action that runs a server-side {@link SerializableRunnable} when its trigger
 * fires. Used by
 * {@link com.vaadin.flow.component.trigger.Trigger#triggers(SerializableRunnable)}.
 * <p>
 * On the client, the {@code flow:server-callback} factory's {@code run()}
 * simply notifies the server via the per-host return channel; the server's
 * {@code TriggerSupport.dispatchMirror} then invokes
 * {@link #applyServerSideEffect()} which calls the wrapped runnable on the UI
 * thread.
 * <p>
 * For internal use only.
 */
public final class ServerCallbackAction extends AbstractAction {

    public static final String TYPE_ID = "flow:server-callback";

    private final SerializableRunnable handler;

    /**
     * @param handler
     *            the server-side handler, not {@code null}
     */
    public ServerCallbackAction(SerializableRunnable handler) {
        super(TYPE_ID);
        this.handler = Objects.requireNonNull(handler);
    }

    /**
     * @return the wrapped handler
     */
    public SerializableRunnable getHandler() {
        return handler;
    }

    @Override
    public void applyServerSideEffect() {
        handler.run();
    }
}
