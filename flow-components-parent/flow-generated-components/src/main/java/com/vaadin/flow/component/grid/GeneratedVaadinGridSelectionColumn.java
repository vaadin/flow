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
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-grid-selection-column>} is a helper element for the
 * {@code <vaadin-grid>} that provides default templates and functionality for
 * item selection.
 * </p>
 * <h4>Example:</h4>
 * <p>
 * &lt;vaadin-grid items=&quot;[[items]]&quot;&gt;
 * &lt;vaadin-grid-selection-column frozen
 * auto-select&gt;&lt;/vaadin-grid-selection-column&gt;
 * </p>
 * <p>
 * &lt;vaadin-grid-column&gt; ...
 * </p>
 * <p>
 * By default the selection column displays {@code <vaadin-checkbox>} elements
 * in the column cells. The checkboxes in the body rows toggle selection of the
 * corresponding row items.
 * </p>
 * <p>
 * When the grid data is provided as an array of <a
 * href="#/elements/vaadin-grid#property-items">{@code items}</a>, the column
 * header gets an additional checkbox that can be used for toggling selection
 * for all the items at once.
 * </p>
 * <p>
 * <strong>The default content can also be overridden</strong>
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.GridSelectionColumnElement#5.2.1",
        "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-grid-selection-column")
@HtmlImport("frontend://bower_components/vaadin-grid/src/vaadin-grid-selection-column.html")
public abstract class GeneratedVaadinGridSelectionColumn<R extends GeneratedVaadinGridSelectionColumn<R>>
        extends GeneratedVaadinGridColumn<R> implements HasStyle {

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

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When true, all the items are selected.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'select-all-changed' event happens.
     * </p>
     * 
     * @return the {@code selectAll} property from the webcomponent
     */
    @Synchronize(property = "selectAll", value = "select-all-changed")
    protected boolean isSelectAllBoolean() {
        return getElement().getProperty("selectAll", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When true, all the items are selected.
     * </p>
     * 
     * @param selectAll
     *            the boolean value to set
     */
    protected void setSelectAll(boolean selectAll) {
        getElement().setProperty("selectAll", selectAll);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When true, the active gets automatically selected.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code autoSelect} property from the webcomponent
     */
    protected boolean isAutoSelectBoolean() {
        return getElement().getProperty("autoSelect", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When true, the active gets automatically selected.
     * </p>
     * 
     * @param autoSelect
     *            the boolean value to set
     */
    protected void setAutoSelect(boolean autoSelect) {
        getElement().setProperty("autoSelect", autoSelect);
    }

    public static class SelectAllChangeEvent<R extends GeneratedVaadinGridSelectionColumn<R>>
            extends ComponentEvent<R> {
        private final boolean selectAll;

        public SelectAllChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.selectAll = source.isSelectAllBoolean();
        }

        public boolean isSelectAll() {
            return selectAll;
        }
    }

    /**
     * Adds a listener for {@code select-all-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addSelectAllChangeListener(
            ComponentEventListener<SelectAllChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("selectAll",
                        event -> listener.onComponentEvent(
                                new SelectAllChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }
}