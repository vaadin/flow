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
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;

/**
 * Component representing a <code>&lt;fieldset&gt;</code> element.
 *
 * This class provides an API for using the HTML fieldset element, which groups
 * related elements in a form. The grouping is visually represented by a border
 * around the elements, and can include a legend at the top defined by the
 * <code>&lt;legend&gt;</code> element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Tag("fieldset")
public class FieldSet extends Div {

    private final Legend legend;
    private final Div content;

    /**
     * Creates a new fieldset with an empty legend.
     */
    public FieldSet() {
        super();
        legend = new Legend();
        content = new Div();
        add(legend, content);
    }

    /**
     * Creates a new fieldset with the given legend text.
     *
     * @param legendText the text for the legend.
     */
    public FieldSet(String legendText) {
        this();
        this.legend.setText(legendText);
    }

    /**
     * Adds a component to the content area of the fieldset.
     *
     * @param component the component to add.
     */
    public void addComponent(Component component) {
        content.add(component);
    }

    /**
     * Returns the Legend component associated with this fieldset.
     *
     * @return the legend component
     */
    public Legend getLegend() {
        return legend;
    }

    /**
     * Sets the text of the legend.
     *
     * @param text the legend text to set.
     */
    public void setLegendText(String text) {
        this.legend.setText(text);
    }
}

/**
 * Component representing a <code>&lt;legend&gt;</code> element.
 *
 * This class encapsulates the HTML legend element that provides a caption for
 * the parent fieldset. It is used to give a title or explanatory text for the
 * group of components within the fieldset.
 *
 * @author Vaadin Ltd
 */
@Tag("legend")
class Legend extends Label {

    /**
     * Creates a new empty legend.
     */
    public Legend() {
        super();
    }

    /**
     * Creates a new legend with the specified text.
     *
     * @param text the text for the legend.
     */
    public Legend(String text) {
        super(text);
    }

    /**
     * Sets the text of the legend.
     *
     * @param text the text to set.
     */
    @Override
    public void setText(String text) {
        getElement().setText(text);
    }
}
