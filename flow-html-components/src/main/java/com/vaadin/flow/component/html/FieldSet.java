/*
 *
 *
 *  * Copyright 2000-2024 Vaadin Ltd.
 *
 *  *
 *
 *  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *
 *  * use this file except in compliance with the License. You may obtain a copy of
 *
 *  * the License at
 *
 *  *
 *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *
 *  *
 *
 *  * Unless required by applicable law or agreed to in writing, software
 *
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *
 *  * License for the specific language governing permissions and limitations under
 *
 *  * the License.
 *
 *
 */

package com.vaadin.flow.component.html;
import java.util.Objects;

import com.vaadin.flow.component.*;

/**
 * Represents an HTML <fieldset> element with a nested <legend>. This component is used to group
 * several UI components within a form. Useful for organizing fields that share a common context.
 * For more complex forms, consider using Vaadin's built-in <a href="https://vaadin.com/docs/latest/components/form-layout">Form Layout</a> component.
 *
 * @see <a href="https://vaadin.com/docs/latest/components/form-layout">Vaadin Form Layout Documentation</a>
 */
@Tag("fieldset")
public class FieldSet extends HtmlComponent implements ClickNotifier<FieldSet> {

    /**
     * Represents an HTML <legend> element.
     */
    @Tag("legend")
    public static class Legend extends HtmlContainer implements ClickNotifier<Legend> {

        /**
         * Creates a new empty legend.
         */
        public Legend() {
            super();
        }

        /**
         * Creates a new legend with text.
         * @param text the text to set as legend.
         */
        public Legend(String text) {
            this();
            setText(text);
        }
    }

    private final Legend legend;
    private Component content;

    /**
     * Creates a new fieldset with an empty legend.
     */
    public FieldSet() {
        super();
        legend = new Legend();
        getElement().appendChild(legend.getElement());
    }

    /**
     * Creates a new fieldset with the given legend text.
     * @param legendText the legend text to set.
     */
    public FieldSet(String legendText) {
        this();
        this.legend.setText(legendText);
    }

    /**
     * Creates a new fieldset with the given content.
     * @param content the content component to set.
     */
    public FieldSet(Component content) {
        this();
        setContent(content);
    }

    /**
     * Creates a new fieldset using the provided legend text and content.
     * @param legendText the legend text to set.
     * @param content the content component to set.
     */
    public FieldSet(String legendText, Component content) {
        this(legendText);
        setContent(content);
    }

    /**
     * Returns the legend component associated with this fieldset.
     * @return the legend component.
     */
    public Legend getLegend() {
        return legend;
    }

    /**
     * Sets the text of the legend.
     * @param text the text to set.
     */
    public void setLegendText(String text) {
        legend.setText(text);
    }

    /**
     * Returns the content of the fieldset.
     * @return the content component, can be null.
     */
    public Component getContent() {
        return content;
    }

    /**
     * Sets the content of the fieldset and removes previously set content.
     * @param content the content of the fieldset to set.
     */
    public void setContent(Component content) {
        Objects.requireNonNull(content, "Content cannot be null");
        if (this.content != null) {
            this.content.getElement().removeFromParent();
        }
        this.content = content;
        getElement().appendChild(content.getElement());
    }
}
