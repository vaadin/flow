/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.component.listbox;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.ComponentSupplier;
import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import elemental.json.JsonArray;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.component.HasComponents;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-list-box>} is a Polymer 2 element for menus
 * </p>
 * <p>
 * {@code
<vaadin-list-box selected="2">
<vaadin-item>Item 1</vaadin-item>
<vaadin-item>Item 2</vaadin-item>
<vaadin-item>Item 3</vaadin-item>
<vaadin-item>Item 4</vaadin-item>
</vaadin-list-box>}
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
 * <td>{@code items}</td>
 * <td>The items container</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * See <a
 * href="https://github.com/vaadin/vaadin-themable-mixin/wiki">ThemableMixin –
 * how to apply styles for shadow parts</a>
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: Vaadin.ListBoxElement#1.0.0-alpha8",
        "Flow#1.0-SNAPSHOT" })
@Tag("vaadin-list-box")
@HtmlImport("frontend://bower_components/vaadin-list-box/src/vaadin-list-box.html")
public class GeneratedVaadinListBox<R extends GeneratedVaadinListBox<R>> extends
        Component implements HasStyle, ComponentSupplier<R>, HasComponents {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The array of list items
     * <p>
     * This property is synchronized automatically from client side when a
     * 'items-changed' event happens.
     * </p>
     * 
     * @return the {@code items} property from the webcomponent
     */
    @Synchronize(property = "items", value = "items-changed")
    protected JsonArray protectedGetItems() {
        return (JsonArray) getElement().getPropertyRaw("items");
    }

    public void focus() {
        getElement().callFunction("focus");
    }

    @DomEvent("items-changed")
    public static class ItemsChangeEvent<R extends GeneratedVaadinListBox<R>>
            extends ComponentEvent<R> {
        private final JsonArray items;

        public ItemsChangeEvent(R source, boolean fromClient,
                @EventData("event.items") JsonArray items) {
            super(source, fromClient);
            this.items = items;
        }

        public JsonArray getItems() {
            return items;
        }
    }

    /**
     * Adds a listener for {@code items-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addItemsChangeListener(
            ComponentEventListener<ItemsChangeEvent<R>> listener) {
        return addListener(ItemsChangeEvent.class,
                (ComponentEventListener) listener);
    }

    /**
     * Adds the given components as children of this component.
     * 
     * @param components
     *            the components to add
     * @see HasComponents#add(Component...)
     */
    public GeneratedVaadinListBox(Component... components) {
        add(components);
    }

    /**
     * Default constructor.
     */
    public GeneratedVaadinListBox() {
    }
}