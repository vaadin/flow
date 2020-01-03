/*
 * Copyright 2000-2020 Vaadin Ltd.
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
 * A {@code <vaadin-grid-column-group>} is used to make groups of columns in
 * {@code <vaadin-grid>} and to configure additional headers and footers.
 * </p>
 * <p>
 * Groups can be nested to create complex header and footer configurations.
 * </p>
 * <p>
 * The {@code class} attribute is used to differentiate header and footer
 * templates.
 * </p>
 * <h4>Example:</h4>
 * <p>
 * &lt;vaadin-grid-column-group resizable&gt; &lt;template
 * class=&quot;header&quot;&gt;Name&lt;/template&gt;
 * </p>
 * <p>
 * &lt;vaadin-grid-column&gt; &lt;template
 * class=&quot;header&quot;&gt;First&lt;/template&gt;
 * &lt;template&gt;[[item.name.first]]&lt;/template&gt;
 * &lt;/vaadin-grid-column&gt; &lt;vaadin-grid-column&gt; &lt;template
 * class=&quot;header&quot;&gt;Last&lt;/template&gt;
 * &lt;template&gt;[[item.name.last]]&lt;/template&gt;
 * &lt;/vaadin-grid-column&gt; &lt;/vaadin-grid-column-group&gt;
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.GridColumnGroupElement#5.2.1",
        "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-grid-column-group")
@HtmlImport("frontend://bower_components/vaadin-grid/src/vaadin-grid-column-group.html")
public abstract class GeneratedVaadinGridColumnGroup<R extends GeneratedVaadinGridColumnGroup<R>>
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
     * Text content to display in the header cell of the column.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code header} property from the webcomponent
     */
    protected String getHeaderString() {
        return getElement().getProperty("header");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Text content to display in the header cell of the column.
     * </p>
     * 
     * @param header
     *            the String value to set
     */
    protected void setHeader(String header) {
        getElement().setProperty("header", header == null ? "" : header);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Aligns the columns cell content horizontally. Supported values:
     * &quot;start&quot;, &quot;center&quot; and &quot;end&quot;.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code textAlign} property from the webcomponent
     */
    protected String getTextAlignString() {
        return getElement().getProperty("textAlign");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Aligns the columns cell content horizontally. Supported values:
     * &quot;start&quot;, &quot;center&quot; and &quot;end&quot;.
     * </p>
     * 
     * @param textAlign
     *            the String value to set
     */
    protected void setTextAlign(String textAlign) {
        getElement().setProperty("textAlign",
                textAlign == null ? "" : textAlign);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Flex grow ratio for the column group as the sum of the ratios of its
     * child columns.
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
     * Width of the column group as the sum of the widths of its child columns.
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
}