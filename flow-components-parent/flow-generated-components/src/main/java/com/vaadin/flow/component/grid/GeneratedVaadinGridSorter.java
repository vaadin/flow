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
 * {@code <vaadin-grid-sorter>} is a helper element for the
 * {@code <vaadin-grid>} that provides out-of-the-box UI controls, visual
 * feedback, and handlers for sorting the grid data.
 * </p>
 * <h4>Example:</h4>
 * <p>
 * &lt;vaadin-grid-column&gt; &lt;template class=&quot;header&quot;&gt;
 * &lt;vaadin-grid-sorter path=&quot;name.first&quot;&gt;First
 * name&lt;/vaadin-grid-sorter&gt; &lt;/template&gt;
 * &lt;template&gt;[[item.name.first]]&lt;/template&gt;
 * &lt;/vaadin-grid-column&gt;
 * </p>
 * <h3>Styling</h3>
 * <p>
 * The following shadow DOM parts are available for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Part name</th>
 * <th>Description</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code content}</td>
 * <td>The slotted content wrapper</td>
 * </tr>
 * <tr>
 * <td>{@code indicators}</td>
 * <td>The internal sorter indicators.</td>
 * </tr>
 * <tr>
 * <td>{@code order}</td>
 * <td>The internal sorter order</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * The following state attributes are available for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Attribute</th>
 * <th>Description</th>
 * <th>Part name</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code direction}</td>
 * <td>Sort direction of a sorter</td>
 * <td>:host</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.GridSorterElement#5.2.1", "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-grid-sorter")
@HtmlImport("frontend://bower_components/vaadin-grid/src/vaadin-grid-sorter.html")
public abstract class GeneratedVaadinGridSorter<R extends GeneratedVaadinGridSorter<R>>
        extends Component implements HasStyle {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * JS Path of the property in the item used for sorting the data.
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
     * JS Path of the property in the item used for sorting the data.
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
     * How to sort the data. Possible values are {@code asc} to use an ascending
     * algorithm, {@code desc} to sort the data in descending direction, or
     * {@code null} for not sorting the data.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'direction-changed' event happens.
     * </p>
     * 
     * @return the {@code direction} property from the webcomponent
     */
    @Synchronize(property = "direction", value = "direction-changed")
    protected String getDirectionString() {
        return getElement().getProperty("direction");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * How to sort the data. Possible values are {@code asc} to use an ascending
     * algorithm, {@code desc} to sort the data in descending direction, or
     * {@code null} for not sorting the data.
     * </p>
     * 
     * @param direction
     *            the String value to set
     */
    protected void setDirection(String direction) {
        getElement().setProperty("direction",
                direction == null ? "" : direction);
    }

    public static class DirectionChangeEvent<R extends GeneratedVaadinGridSorter<R>>
            extends ComponentEvent<R> {
        private final String direction;

        public DirectionChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.direction = source.getDirectionString();
        }

        public String getDirection() {
            return direction;
        }
    }

    /**
     * Adds a listener for {@code direction-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addDirectionChangeListener(
            ComponentEventListener<DirectionChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("direction",
                        event -> listener.onComponentEvent(
                                new DirectionChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }
}