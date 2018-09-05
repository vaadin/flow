/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.component.grid;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Component;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * A {@code <vaadin-grid-column>} is used to configure how a column in
 * {@code <vaadin-grid>} should look like by using HTML templates. A column can
 * have a template for each of the three table sections: header, body and
 * footer.
 * </p>
 * <p>
 * The {@code class} attribute is used to differentiate header and footer
 * templates from the body template.
 * </p>
 * <h4>Example:</h4>
 * <p>
 * &lt;vaadin-grid-column&gt; &lt;template class=&quot;header&quot;&gt;I'm in
 * the header&lt;/template&gt; &lt;template&gt;I'm in the body&lt;/template&gt;
 * &lt;template class=&quot;footer&quot;&gt;I'm in the footer&lt;/template&gt;
 * &lt;/vaadin-grid-column&gt;
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.1-SNAPSHOT",
        "WebComponent: Vaadin.GridColumnElement#5.1.0-alpha3",
        "Flow#1.1-SNAPSHOT" })
@Tag("vaadin-grid-column")
@HtmlImport("frontend://bower_components/vaadin-grid/src/vaadin-grid-column.html")
public abstract class GeneratedVaadinGridColumn<R extends GeneratedVaadinGridColumn<R>>
        extends Component implements HasStyle {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When set to true, the column is user-resizable.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code resizable} property from the webcomponent
     */
    protected boolean isResizableBoolean() {
        return getElement().getProperty("resizable", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When set to true, the column is user-resizable.
     * </p>
     * 
     * @param resizable
     *            the boolean value to set
     */
    protected void setResizable(boolean resizable) {
        getElement().setProperty("resizable", resizable);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When true, the column is frozen. When a column inside of a column group
     * is frozen, all of the sibling columns inside the group will get frozen
     * also.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code frozen} property from the webcomponent
     */
    protected boolean isFrozenBoolean() {
        return getElement().getProperty("frozen", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When true, the column is frozen. When a column inside of a column group
     * is frozen, all of the sibling columns inside the group will get frozen
     * also.
     * </p>
     * 
     * @param frozen
     *            the boolean value to set
     */
    protected void setFrozen(boolean frozen) {
        getElement().setProperty("frozen", frozen);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When set to true, the cells for this column are hidden.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code hidden} property from the webcomponent
     */
    protected boolean isHiddenBoolean() {
        return getElement().getProperty("hidden", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When set to true, the cells for this column are hidden.
     * </p>
     * 
     * @param hidden
     *            the boolean value to set
     */
    protected void setHidden(boolean hidden) {
        getElement().setProperty("hidden", hidden);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Width of the cells for this column.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code width} property from the webcomponent
     */
    protected String getWidthString() {
        return getElement().getProperty("width");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Width of the cells for this column.
     * </p>
     * 
     * @param width
     *            the String value to set
     */
    protected void setWidth(String width) {
        getElement().setProperty("width", width == null ? "" : width);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Flex grow ratio for the cell widths. When set to 0, cell width is fixed.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code flexGrow} property from the webcomponent
     */
    protected double getFlexGrowDouble() {
        return getElement().getProperty("flexGrow", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Flex grow ratio for the cell widths. When set to 0, cell width is fixed.
     * </p>
     * 
     * @param flexGrow
     *            the double value to set
     */
    protected void setFlexGrow(double flexGrow) {
        getElement().setProperty("flexGrow", flexGrow);
    }
}