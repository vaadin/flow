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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableConsumer;

/**
 * Configuration for
 * {@link Clipboard#onPaste(Component, PasteOptions, SerializableConsumer)
 * Clipboard.onPaste}.
 *
 * @param includeInputFieldPastes
 *            whether to forward pastes whose composed path crosses an editable
 *            target ({@code <input>}, {@code <textarea>}, or an element with
 *            {@code contenteditable}). The {@link #defaults()} factory sets
 *            this to {@code false} &mdash; pastes into a focused form field,
 *            including a focused field inside a Vaadin web component's shadow
 *            DOM, are skipped &mdash; which is typically what page-wide
 *            listeners want. The {@link #includingInputFields()} factory sets
 *            this to {@code true}, so the listener observes every paste
 *            regardless of focus; this is also the default when no options are
 *            passed to
 *            {@link Clipboard#onPaste(Component, SerializableConsumer)}.
 */
public record PasteOptions(
        boolean includeInputFieldPastes) implements Serializable {

    /**
     * Returns the default options: editable-target pastes are skipped.
     *
     * @return default options, equivalent to {@code new PasteOptions(false)}
     */
    public static PasteOptions defaults() {
        return new PasteOptions(false);
    }

    /**
     * @return options that also forward pastes targeting input fields, text
     *         areas, and {@code contenteditable} elements; equivalent to
     *         {@code new PasteOptions(true)}.
     */
    public static PasteOptions includingInputFields() {
        return new PasteOptions(true);
    }
}
