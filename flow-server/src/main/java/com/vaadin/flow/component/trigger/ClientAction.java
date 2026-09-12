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

import java.io.Serializable;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.Registration;

/**
 * A browser-side action — copy to the clipboard, share, enter fullscreen, start
 * a download — that is not yet bound to anything that fires it.
 * <p>
 * Ordinary trigger bindings name the component that fires them
 * ({@code Clipboard.onClick(button)}), which requires a server-side component
 * per binding. A {@code ClientAction} instead describes only <em>what</em>
 * should happen, so it can be handed to something that renders its own elements
 * on the client — a {@code LitRenderer} template, for example — and fired from
 * there once per rendered element while still costing one binding:
 *
 * <pre>{@code
 * grid.addColumn(LitRenderer.<Customer> of(
 *         "<span>${item.email}</span><button @click=${copy}>Copy</button>")
 *         .withProperty("email", Customer::email).withClientAction("copy",
 *                 Clipboard.write().text(ClientValue.itemProperty("email"))));
 * }</pre>
 *
 * The action runs inside the browser's own event handler, so the user gesture
 * is still valid — the whole reason clipboard, share and fullscreen calls
 * cannot be made from a server-side listener.
 * <p>
 * Instances are created by the feature facades ({@code Clipboard.write()},
 * {@code WebShare.share(…)}, …), not by application code.
 *
 * @see ClientValue
 */
public interface ClientAction extends Serializable {

    /**
     * Binds this action to {@code host} and hands the rendered client-side
     * function to {@code sink}, which decides when it runs.
     * <p>
     * Called by the component or renderer that accepts the action, once per
     * place it is rendered into. The returned {@link Registration} detaches the
     * binding.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @param host
     *            the element whose lifecycle the binding belongs to, not
     *            {@code null}
     * @param sink
     *            receives the rendered action function, not {@code null}
     * @return a registration that detaches the binding, never {@code null}
     */
    Registration bindTo(Element host, ClientActionSink sink);
}
