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
package com.vaadin.flow.component.html;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasAriaLabel;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.signals.Signal;

/**
 * Represents an HTML <code>&lt;fieldset&gt;</code> element. This component is
 * used to group several UI components within a form, enhancing form
 * accessibility and organization.
 */
@Tag("fieldset")
public class FieldSet extends HtmlContainer implements HasAriaLabel {

    /**
     * Represents an HTML <code>&lt;legend&gt;</code> element.
     */
    @Tag("legend")
    public static class Legend extends HtmlContainer {

        /**
         * Creates a new empty legend.
         */
        public Legend() {
            super();
        }

        /**
         * Creates a new legend with text.
         *
         * @param text
         *            the text to set as legend.
         */
        public Legend(String text) {
            this();
            setText(text);
        }

        /**
         * Creates a new legend with its text content bound to the given signal.
         *
         * @param textSignal
         *            the signal to bind the legend text to, not {@code null}
         * @see #bindText(Signal)
         */
        public Legend(Signal<String> textSignal) {
            Objects.requireNonNull(textSignal, "textSignal must not be null");
            bindText(textSignal);
        }
    }

    /**
     * Creates a new fieldset with an empty legend.
     */
    public FieldSet() {
        super();
    }

    /**
     * Creates a new fieldset with the given legend text.
     *
     * @param legendText
     *            the legend text to set.
     */
    public FieldSet(String legendText) {
        this();
        if (legendText != null && !legendText.isEmpty()) {
            addComponentAsFirst(new Legend(legendText));
        }
    }

    /**
     * Creates a new fieldset with its legend text bound to the given signal.
     *
     * @param textSignal
     *            the legend text signal to bind to, not {@code null}
     * @see #bindText(Signal)
     */
    public FieldSet(Signal<String> textSignal) {
        addComponentAsFirst(new Legend(textSignal));
    }

    /**
     * Creates a new fieldset with the given content.
     *
     * @param content
     *            the content component to set.
     */
    public FieldSet(Component... content) {
        super(content);
    }

    /**
     * Creates a new fieldset using the provided legend text and content.
     *
     * @param legendText
     *            the legend text to set.
     * @param content
     *            the content component to set.
     */
    public FieldSet(String legendText, Component content) {
        this(legendText);
        add(content);
    }

    /**
     * Returns the legend component associated with this fieldset.
     *
     * @return the legend component.
     */
    public Legend getLegend() {
        return findLegend();
    }

    /**
     * Sets the text of the legend.
     *
     * @param text
     *            the text to set.
     */
    public void setLegendText(String text) {
        Legend legend = findLegend();
        if (text != null && !text.isEmpty()) {
            if (legend == null) {
                legend = new Legend(text);
                addComponentAsFirst(legend);
            } else {
                legend.setText(text);
            }
        } else if (legend != null) {
            remove(legend);
        }
    }

    /**
     * Gets the text of the legend.
     *
     * @return the text of the legend, or null if no legend is present.
     */
    public String getLegendText() {
        Legend legend = findLegend();
        return (legend != null) ? legend.getText() : null;
    }

    /**
     * Binds a signal's value to the fieldset legend text so that the legend is
     * updated when the signal's value is updated.
     * <p>
     * Passing {@code null} as the {@code signal} removes any existing binding
     * for the legend text. When unbinding, the current legend text is left
     * unchanged.
     * <p>
     * While a binding for the legend text is active, any attempt to set the
     * legend text manually via {@link #setLegendText(String)} throws
     * {@link com.vaadin.flow.signals.BindingActiveException}. The same happens
     * when trying to bind a new Signal while one is already bound.
     * <p>
     * Bindings are lifecycle-aware and only active while this component is in
     * the attached state; they are deactivated while the component is in the
     * detached state.
     *
     * @param legendTextSignal
     *            the signal to bind or <code>null</code> to unbind any existing
     *            binding
     * @throws com.vaadin.flow.signals.BindingActiveException
     *             thrown when there is already an existing binding
     * @see #setLegendText(String)
     * @see com.vaadin.flow.component.HasText#bindText(Signal)
     *
     * @since 25.1
     */
    public void bindLegendText(Signal<String> legendTextSignal) {
        Legend legend = findLegend();
        if (legendTextSignal != null) {
            if (legend == null) {
                legend = new Legend();
                addComponentAsFirst(legend);
            }
            legend.bindText(legendTextSignal);
        } else if (legend != null) {
            // Unbind existing binding but keep current value and legend element
            legend.bindText(null);
        }
    }

    /**
     * Returns the content of the fieldset.
     *
     * @return Stream of content components
     */
    public Stream<Component> getContent() {
        return getChildren().filter(c -> !(c instanceof Legend));
    }

    /**
     * Sets the content of the fieldset and removes previously set content.
     *
     * Note: Do not include Legend in the content components. Use other FieldSet
     * methods for setting Legend instead.
     *
     * @param content
     *            the content components of the fieldset to set.
     */
    public void setContent(Component... content) {
        Objects.requireNonNull(content, "Content should not be null");
        for (Component c : content) {
            if (c instanceof Legend) {
                throw new IllegalArgumentException(
                        "Legend should not be included in the content. "
                                + "Use constructor params or setLegend.. methods instead.");
            }
        }
        removeAll();
        Legend legend = findLegend();
        if (legend != null) {
            addComponentAsFirst(legend);
        }
        add(content);
    }

    private Legend findLegend() {
        Optional<Component> legend = getChildren()
                .filter(c -> c instanceof Legend).findFirst();
        return (Legend) legend.orElse(null);
    }

}
