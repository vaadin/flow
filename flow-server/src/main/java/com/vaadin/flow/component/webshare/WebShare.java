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
package com.vaadin.flow.component.webshare;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.signals.Signal;

/**
 * Entry point for the browser's
 * <a href= "https://developer.mozilla.org/en-US/docs/Web/API/Web_Share_API">Web
 * Share API</a> ({@code navigator.share}). Two entry points:
 * <ul>
 * <li>{@link #onClick(Component)} — bind a share action to a click gesture. The
 * Web Share API requires a transient user gesture, so the trigger pattern is
 * the only reliable way to invoke it.</li>
 * <li>{@link #supportSignal()} — feature detection: read whether the current
 * browser exposes {@code navigator.share}, useful for deciding whether to show
 * a share affordance at all.</li>
 * </ul>
 *
 * <pre>{@code
 * Button share = new Button("Share");
 * if (WebShare.supportSignal().peek() == WebShareSupport.SUPPORTED) {
 *     WebShare.onClick(share).share(
 *             ShareContent.create().title("Vaadin").url("https://vaadin.com"));
 * }
 * }</pre>
 *
 * The Web Share API requires a fresh user gesture for each call, so actions
 * only run during the DOM event that fires the underlying trigger.
 */
public final class WebShare implements Serializable {

    private WebShare() {
        // utility class
    }

    /**
     * Registers the given component as a clickable trigger for a share action —
     * the common shape for "Share" buttons. Equivalent to
     * {@code new ClickTrigger(component)}, without making callers reach for the
     * trigger framework's internal types.
     *
     * @param component
     *            the component to listen for clicks on, not {@code null}
     * @param <T>
     *            the component type, must implement {@link ClickNotifier}
     * @return a new binding that can chain actions to this trigger
     */
    public static <T extends Component & ClickNotifier<?>> WebShareBinding onClick(
            T component) {
        Objects.requireNonNull(component, "component must not be null");
        return new WebShareBinding(new ClickTrigger(component));
    }

    /**
     * Returns a read-only signal hinting at whether the Web Share API is
     * available in the current browser for the current UI. The value is seeded
     * from the bootstrap handshake, so user code observes a real value before
     * any view code runs.
     * <p>
     * Web Share support is established at page load and does not change during
     * the session, so the signal effectively transitions {@code UNKNOWN} →
     * {@code SUPPORTED}/{@code UNSUPPORTED} once and then remains stable.
     * <p>
     * Use this to decide whether to show a share affordance at all — calling a
     * share action when the value is {@link WebShareSupport#UNSUPPORTED}
     * silently rejects in the browser.
     *
     * @return the read-only support signal
     * @throws IllegalStateException
     *             if there is no current UI
     */
    public static Signal<WebShareSupport> supportSignal() {
        return supportSignal(UI.getCurrentOrThrow());
    }

    /**
     * Returns a read-only signal hinting at whether the Web Share API is
     * available in the current browser for the given UI. Same semantics as
     * {@link #supportSignal()}; use this overload from background threads or
     * anywhere {@link UI#getCurrent()} is unreliable.
     *
     * @param ui
     *            the UI to read the hint from, not {@code null}
     * @return the read-only support signal
     * @throws NullPointerException
     *             if {@code ui} is {@code null}
     */
    public static Signal<WebShareSupport> supportSignal(UI ui) {
        Objects.requireNonNull(ui, "ui must not be null");
        return ui.getInternals().getWebShareSupportSignal().asReadonly();
    }
}
