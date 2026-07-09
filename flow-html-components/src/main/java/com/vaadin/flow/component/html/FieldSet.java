/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.flow.component.*;

/**
 * Represents an HTML <code>&lt;fieldset&gt;</code> element. This component is
 * used to group several UI components within a form, enhancing form
 * accessibility and organization.
 *
 * @since 24.5
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
