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
package com.vaadin.flow.component.clipboard;

import java.io.Serializable;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.trigger.ClientAction;
import com.vaadin.flow.component.trigger.ClientValue;
import com.vaadin.flow.component.trigger.internal.ClientActions;
import com.vaadin.flow.component.trigger.internal.PromiseAction.Error;
import com.vaadin.flow.component.trigger.internal.WriteToClipboardAction;
import com.vaadin.flow.function.SerializableConsumer;

/**
 * Fluent surface returned from {@link Clipboard#write()}, used to declare what
 * a clipboard write copies when something on the client fires it.
 * <p>
 * Unlike {@link Clipboard#onClick(com.vaadin.flow.component.Component)}, the
 * write declared here is not bound to a component: it produces a
 * {@link ClientAction} that whatever renders the affordance fires — typically a
 * renderer that draws the same affordance for every item it renders.
 *
 * <pre>{@code
 * LitRenderer.<Customer> of(
 *         "<span>${item.email}</span><button @click=${copy}>Copy</button>")
 *         .withProperty("email", Customer::email).withClientAction("copy",
 *                 Clipboard.write().text(ClientValue.itemProperty("email")));
 * }</pre>
 */
public final class ClipboardWrite implements Serializable {

    ClipboardWrite() {
        // Created by Clipboard.write()
    }

    /**
     * Copies the given value to the clipboard as {@code text/plain}. The value
     * is read on the client at the moment the action fires, which is what lets
     * one action serve every item a renderer draws.
     *
     * @param value
     *            the value to copy, not {@code null}
     * @return an unbound action, to be handed to whatever fires it
     */
    public ClientAction text(ClientValue<String> value) {
        Objects.requireNonNull(value, "value must not be null");
        return ClientActions
                .of(new WriteToClipboardAction(value.getInput(), null));
    }

    /**
     * Like {@link #text(ClientValue)} but reports the outcome back to the
     * server.
     *
     * @param value
     *            the value to copy, not {@code null}
     * @param onCopied
     *            UI-thread callback receiving the copied string, not
     *            {@code null}
     * @param onError
     *            UI-thread callback receiving the browser's error, not
     *            {@code null}
     * @return an unbound action, to be handed to whatever fires it
     */
    public ClientAction text(ClientValue<String> value,
            SerializableConsumer<@Nullable String> onCopied,
            SerializableConsumer<Error> onError) {
        Objects.requireNonNull(value, "value must not be null");
        return ClientActions.of(new WriteToClipboardAction(value.getInput(),
                null, onCopied, onError));
    }
}
