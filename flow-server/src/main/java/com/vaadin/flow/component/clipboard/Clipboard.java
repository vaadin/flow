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

import java.util.Objects;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;

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
 */
public final class Clipboard {

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
}
