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
import com.vaadin.flow.component.Component;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * This Element is used internally by vaadin-grid.
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: GridScrollerElement#UNKNOWN", "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-grid-scroller")
@HtmlImport("frontend://bower_components/vaadin-grid/src/vaadin-grid-scroller.html")
public abstract class GeneratedVaadinGridScroller<R extends GeneratedVaadinGridScroller<R>>
        extends Component implements HasStyle {

    /**
     * This property is synchronized automatically from client side when a
     * 'size-changed' event happens.
     * 
     * @return the {@code size} property from the webcomponent
     */
    @Synchronize(property = "size", value = "size-changed")
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

    public static class SizeChangeEvent<R extends GeneratedVaadinGridScroller<R>>
            extends ComponentEvent<R> {
        private final double size;

        public SizeChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.size = source.getSizeDouble();
        }

        public double getSize() {
            return size;
        }
    }

    /**
     * Adds a listener for {@code size-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addSizeChangeListener(
            ComponentEventListener<SizeChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("size",
                        event -> listener.onComponentEvent(
                                new SizeChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }
}