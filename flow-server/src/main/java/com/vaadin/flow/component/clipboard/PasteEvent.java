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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;

/**
 * Server-side representation of a browser {@code paste} event delivered to a
 * listener registered with
 * {@link Clipboard#onPaste(Component, com.vaadin.flow.function.SerializableConsumer)
 * Clipboard.onPaste}.
 * <p>
 * The event carries the two textual MIME types the browser exposes on
 * {@code event.clipboardData}: {@code text/plain} via {@link #getText()} and
 * {@code text/html} via {@link #getHtml()}. Either may be {@code null} when the
 * paste did not include that representation. {@link #getTargetElement()}
 * resolves to the closest Flow-tracked {@link Element} ancestor of the paste's
 * DOM target.
 *
 * <h2>Empty vs. absent</h2>
 *
 * The browser returns the empty string {@code ""} both when the requested MIME
 * type is not present on the clipboard and when the user pasted an empty string
 * of that type. There is no way to distinguish the two. {@code PasteEvent}
 * collapses both into {@code null}, so callers can simply check
 * {@code event.hasHtml()} (or {@code event.getHtml() != null}) without an extra
 * {@code !isEmpty()} guard.
 *
 * <h2>What is not on it</h2>
 *
 * Files and binary clipboard items (pasted screenshots, files dragged from the
 * OS file picker, anything that arrives as {@code event.clipboardData.files} or
 * a {@code DataTransferItem} of kind {@code "file"}) are not delivered by this
 * event. File-paste support is tracked separately.
 *
 * <h2>Browser caveats</h2>
 *
 * <ul>
 * <li>The browser only fires {@code paste} when the target element is focused
 * at the moment the user invokes paste. Non-editable elements (such as a plain
 * {@code Div}) need to be made focusable &mdash; typically via
 * {@code tabindex="0"} &mdash; before they will receive paste events.</li>
 * <li>On editable targets ({@code <input>}, {@code <textarea>}, elements with
 * {@code contenteditable}), the browser still performs its native paste
 * insertion. {@code onPaste} does <em>not</em> call {@code preventDefault()};
 * the listener observes the paste, it does not replace it.</li>
 * <li>On Safari, some plain-text pastes do not include a {@code text/html}
 * representation even when Chromium would synthesise a wrapper, so
 * {@link #getHtml()} may be {@code null} on Safari for the same paste that
 * yields a non-null HTML value on Chrome.</li>
 * </ul>
 *
 * The listener runs on the UI thread.
 */
public final class PasteEvent implements Serializable {

    private final Component source;
    private final @Nullable String text;
    private final @Nullable String html;
    private final @Nullable Element targetElement;

    PasteEvent(Component source, @Nullable String text, @Nullable String html,
            @Nullable Element targetElement) {
        this.source = Objects.requireNonNull(source);
        this.text = text;
        this.html = html;
        this.targetElement = targetElement;
    }

    /**
     * Returns the component the paste listener was registered on &mdash; the
     * component passed to
     * {@link Clipboard#onPaste(Component, com.vaadin.flow.function.SerializableConsumer)
     * Clipboard.onPaste}. For listeners registered against a
     * {@link com.vaadin.flow.component.UI UI} this is the UI itself.
     *
     * @return the source component, never {@code null}
     */
    public Component getSource() {
        return source;
    }

    /**
     * Returns the closest Flow-tracked {@link Element} ancestor of the
     * browser's paste target. The ancestor walk is performed by the browser
     * against the live DOM (not the server-side state tree), so the result
     * reflects the DOM hierarchy the user actually pasted into. For pastes
     * inside a Vaadin web component this is typically the web component's host
     * element, not an internal shadow-DOM child.
     * <p>
     * Use {@link Element#getComponent()} to look up the enclosing
     * {@link Component}, if any. Returns {@code null} only in the rare case
     * that no Flow-tracked element encloses the paste target in the DOM.
     *
     * @return the closest enclosing Flow-tracked element, or {@code null} if
     *         Flow could not resolve one
     */
    public @Nullable Element getTargetElement() {
        return targetElement;
    }

    /**
     * Returns the {@code text/plain} content of the paste, or {@code null} if
     * the paste did not include a non-empty plain-text representation.
     *
     * @return the pasted plain text, or {@code null}
     */
    public @Nullable String getText() {
        return text;
    }

    /**
     * Returns the {@code text/html} content of the paste, or {@code null} if
     * the paste did not include a non-empty HTML representation.
     *
     * @return the pasted HTML, or {@code null}
     */
    public @Nullable String getHtml() {
        return html;
    }

    /**
     * Returns whether the paste included a non-empty {@code text/plain}
     * representation.
     *
     * @return {@code true} if {@link #getText()} is non-{@code null}
     */
    public boolean hasText() {
        return text != null;
    }

    /**
     * Returns whether the paste included a non-empty {@code text/html}
     * representation.
     *
     * @return {@code true} if {@link #getHtml()} is non-{@code null}
     */
    public boolean hasHtml() {
        return html != null;
    }
}
