/*
 * Copyright 2000-2019 Vaadin Ltd.
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

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-grid-tree-column>} is a helper element for the
 * {@code <vaadin-grid>} that provides default template and functionality for
 * toggling tree/hierarchical items.
 * </p>
 * <h4>Example:</h4>
 * <p>
 * &lt;vaadin-grid items=&quot;[[items]]&quot;&gt; &lt;vaadin-grid-tree-column
 * path=&quot;name.first&quot;&gt;&lt;/vaadin-grid-tree-column&gt;
 * </p>
 * <p>
 * &lt;vaadin-grid-column&gt; ...
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.GridTreeColumnElement#5.2.1",
        "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-grid-tree-column")
@HtmlImport("frontend://bower_components/vaadin-grid/src/vaadin-grid-tree-column.html")
public abstract class GeneratedVaadinGridTreeColumn<R extends GeneratedVaadinGridTreeColumn<R>>
        extends GeneratedVaadinGridColumn<R> implements HasStyle {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * JS Path of the property in the item used as text content for the tree
     * toggle.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code path} property from the webcomponent
     */
    protected String getPathString() {
        return getElement().getProperty("path");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * JS Path of the property in the item used as text content for the tree
     * toggle.
     * </p>
     * 
     * @param path
     *            the String value to set
     */
    protected void setPath(String path) {
        getElement().setProperty("path", path == null ? "" : path);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * JS Path of the property in the item that indicates whether the item has
     * child items.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code itemHasChildrenPath} property from the webcomponent
     */
    protected String getItemHasChildrenPathString() {
        return getElement().getProperty("itemHasChildrenPath");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * JS Path of the property in the item that indicates whether the item has
     * child items.
     * </p>
     * 
     * @param itemHasChildrenPath
     *            the String value to set
     */
    protected void setItemHasChildrenPath(String itemHasChildrenPath) {
        getElement().setProperty("itemHasChildrenPath",
                itemHasChildrenPath == null ? "" : itemHasChildrenPath);
    }
}