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
 * This Element is used internally by vaadin-grid.
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.1-SNAPSHOT",
        "WebComponent: GridScrollerElement#UNKNOWN", "Flow#1.1-SNAPSHOT" })
@Tag("vaadin-grid-scroller")
@HtmlImport("frontend://bower_components/vaadin-grid/src/vaadin-grid-scroller.html")
public abstract class GeneratedVaadinGridScroller<R extends GeneratedVaadinGridScroller<R>>
        extends Component implements HasStyle {

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code size} property from the webcomponent
     */
    protected double getSizeDouble() {
        return getElement().getProperty("size", 0.0);
    }

    /**
     * @param size
     *            the double value to set
     */
    protected void setSize(double size) {
        getElement().setProperty("size", size);
    }

    protected void clearSelection() {
        getElement().callFunction("clearSelection");
    }

    protected void updateViewportBoundaries() {
        getElement().callFunction("updateViewportBoundaries");
    }
}