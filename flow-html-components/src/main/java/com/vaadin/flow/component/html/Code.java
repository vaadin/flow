/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.component.html;

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;
import com.vaadin.signals.Signal;

/**
 * Component representing a <code>&lt;code&gt;</code> element.
 *
 * @author Vaadin Ltd
 * @since 25.0
 */
@Tag(Tag.CODE)
public class Code extends HtmlContainer {

    /**
     * Creates a new empty code component.
     */
    public Code() {
        super();
    }

    /**
     * Creates a new code component with the given child components.
     *
     * @param components
     *            the child components
     */
    public Code(Component... components) {
        super(components);
    }

    /**
     * Creates a new code component with the given text.
     *
     * @param text
     *            the text
     */
    public Code(String text) {
        super();
        setText(text);
    }

    /**
     * Creates a new code component with its text content bound to the given
     * signal.
     *
     * @param textSignal
     *            the signal to bind, not {@code null}
     * @see #bindText(Signal)
     */
    public Code(Signal<String> textSignal) {
        Objects.requireNonNull(textSignal, "textSignal must not be null");
        bindText(textSignal);
    }
}
