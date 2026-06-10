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

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;

/**
 * Entry point for the browser clipboard API. Bind clipboard actions to a user
 * gesture by chaining off {@link #onClick(Component)}:
 *
 * <pre>{@code
 * Button copyButton = new Button("Copy");
 * Clipboard.onClick(copyButton).writeText(textField);
 *
 * Clipboard.onClick(copyButton)
 *         .write(ClipboardContent.create().text("Hello").html("<b>Hello</b>"));
 * }</pre>
 *
 * The Clipboard API requires a fresh user gesture for each write, so actions
 * only run during the DOM event that fires the underlying trigger.
 * <p>
 * Read-side support is exposed through
 * {@link #onPaste(Component, SerializableConsumer) onPaste}, which forwards the
 * browser's native {@code paste} event to a server-side listener as a
 * {@link PasteEvent}. Unlike the write API, {@code onPaste} does not need a
 * click binding &mdash; it attaches a DOM listener directly to the given
 * component and fires on every paste gesture targeting it (or any of its
 * descendants, since {@code paste} bubbles). Pass the
 * {@link com.vaadin.flow.component.UI UI} as the component for UI-wide scope;
 * the {@link #onPaste(Component, PasteOptions, SerializableConsumer) options
 * overload} lets the application skip pastes whose target is a form field
 * &mdash; useful for page-wide listeners that should only react to pastes
 * intended for the page as a whole.
 */
public final class Clipboard implements Serializable {

    private Clipboard() {
        // utility class
    }

    /**
     * Registers the given component as a clickable trigger for a clipboard
     * action — the common shape for copy-to-clipboard buttons. Equivalent to
     * {@code new ClickTrigger(component)}, without making callers reach for the
     * trigger framework's internal types.
     *
     * @param component
     *            the component to listen for clicks on, not {@code null}
     * @param <T>
     *            the component type, must implement {@link ClickNotifier}
     * @return a new binding that can chain actions to this trigger
     */
    public static <T extends Component & ClickNotifier<?>> ClipboardBinding onClick(
            T component) {
        Objects.requireNonNull(component, "component must not be null");
        return new ClipboardBinding(new ClickTrigger(component));
    }

    /**
     * Registers a listener for browser {@code paste} events on the given
     * component. The listener is invoked on the UI thread once per paste
     * gesture targeting {@code component} (or any descendant, since
     * {@code paste} bubbles) with a {@link PasteEvent} carrying the
     * {@code text/plain} and {@code text/html} representations of the pasted
     * content. For UI-wide scope, pass the {@link com.vaadin.flow.component.UI
     * UI} as the component; the UI's root element is {@code <body>} so it
     * receives every paste event that bubbles up from anywhere on the page.
     *
     * <p>
     * The browser only fires {@code paste} when the target element is focused
     * at the moment the user invokes paste. For non-editable elements such as a
     * {@code Div} this means the element must be made focusable, typically via
     * {@code tabindex="0"}. See {@link PasteEvent} for the rest of the browser
     * caveats.
     *
     * <p>
     * Example: a drop zone the user pastes a license key into.
     *
     * <pre>{@code
     * Div dropZone = new Div("Drop a key file or paste your license key");
     * dropZone.addClassName("license-drop-zone");
     * dropZone.getElement().setAttribute("tabindex", "0");
     * add(dropZone);
     *
     * Clipboard.onPaste(dropZone, event -> {
     *     String key = event.getText();
     *     if (key == null || key.isBlank()) {
     *         dropZone.setText("Paste did not contain a license key");
     *     } else {
     *         activateLicense(key.strip());
     *         dropZone.setText("License key applied");
     *     }
     * });
     * }</pre>
     *
     * @param component
     *            the component to listen for paste events on, not {@code null}
     * @param listener
     *            the listener invoked for each paste, not {@code null}
     * @return a {@link Registration} whose {@link Registration#remove() remove}
     *         detaches the paste listener
     */
    public static Registration onPaste(Component component,
            SerializableConsumer<PasteEvent> listener) {
        return onPaste(component, PasteOptions.includingInputFields(),
                listener);
    }

    /**
     * Registers a listener for browser {@code paste} events on the given
     * component with the given {@link PasteOptions}. The listener is invoked on
     * the UI thread for each paste gesture targeting {@code component} (or any
     * descendant, since {@code paste} bubbles) whose target matches the
     * options. For UI-wide scope, pass the {@link com.vaadin.flow.component.UI
     * UI} as the component; the UI's root element is {@code <body>} so it
     * receives every paste event that bubbles up from anywhere on the page.
     * <p>
     * The component does not need to be attached at registration time — the
     * underlying DOM listener is bound to the component's element and is
     * applied when the element is attached to a UI.
     * <p>
     * Pass {@link PasteOptions#defaults()} to skip pastes whose target is an
     * input, textarea, or {@code contenteditable} element (typically what a
     * page-wide listener wants). Pass
     * {@link PasteOptions#includingInputFields()} to observe every paste
     * regardless of focus.
     *
     * @param component
     *            the component to listen for paste events on, not {@code null}
     * @param options
     *            paste filtering options, not {@code null}
     * @param listener
     *            the listener invoked for each matching paste, not {@code null}
     * @return a {@link Registration} whose {@link Registration#remove() remove}
     *         detaches the paste listener
     */
    public static Registration onPaste(Component component,
            PasteOptions options, SerializableConsumer<PasteEvent> listener) {
        Objects.requireNonNull(component, "component must not be null");
        Objects.requireNonNull(options, "options must not be null");
        Objects.requireNonNull(listener, "listener must not be null");
        return PasteEventDispatcher.register(component, options, listener);
    }
}
