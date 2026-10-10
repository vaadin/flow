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

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.trigger.ClientAction;
import com.vaadin.flow.component.trigger.ClientActionSink;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.Registration;

/**
 * Bridges the public {@link ClientAction} handle to the internal {@link Action}
 * it carries, so feature facades (clipboard, fullscreen, share, …) can hand an
 * unbound action to whatever renders it without exposing the trigger internals.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class ClientActions implements Serializable {

    private ClientActions() {
        // utility class
    }

    /**
     * Wraps an internal action as a public {@link ClientAction} handle. Binding
     * the handle creates a {@link SinkTrigger} on the host element and wires
     * the action to it.
     *
     * @param action
     *            the action to run when whatever renders the handle fires it,
     *            not {@code null}
     * @return an unbound client action handle
     */
    public static ClientAction of(Action action) {
        Objects.requireNonNull(action, "action must not be null");
        return new ClientAction() {
            @Override
            public Registration bindTo(Element host, ClientActionSink sink) {
                SinkTrigger trigger = new SinkTrigger(host, sink);
                trigger.triggers(action);
                return trigger::remove;
            }
        };
    }
}
