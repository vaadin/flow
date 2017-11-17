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
package com.vaadin.ui.radiobutton;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
import com.vaadin.ui.common.Focusable;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.event.Synchronize;
import com.vaadin.ui.event.DomEvent;
import com.vaadin.ui.event.ComponentEvent;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.shared.Registration;
import com.vaadin.ui.common.HasComponents;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-radio-button>} is a Polymer element for radio buttons.
 * </p>
 * <p>
 * {@code }
 * <code>html &lt;vaadin-radio-button value=&quot;foo&quot;&gt;Foo&lt;/vaadin-radio-button&gt; {@code }</code>
 * </p>
 * <h3>Styling</h3>
 * <p>
 * <a href=
 * "https://cdn.vaadin.com/vaadin-valo-theme/0.3.1/demo/customization.html"
 * >Generic styling/theming documentation</a>
 * </p>
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
 * <td>{@code wrapper}</td>
 * <td>The {@code <label>} element which wrapps the radio button and
 * [part=&quot;label&quot;]</td>
 * </tr>
 * <tr>
 * <td>{@code native-radio}</td>
 * <td>The {@code <input type="radio">} element</td>
 * </tr>
 * <tr>
 * <td>{@code radio}</td>
 * <td>The {@code <span>} element for a custom graphical radio button</td>
 * </tr>
 * <tr>
 * <td>{@code label}</td>
 * <td>The {@code <span>} element for slotted text/HTML label</td>
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
 * <td>{@code disabled}</td>
 * <td>Set when the radio button is disabled.</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code focus-ring}</td>
 * <td>Set when the radio button is focused using the keyboard.</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code focused}</td>
 * <td>Set when the radio button is focused.</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code checked}</td>
 * <td>Set when the radio button is checked.</td>
 * <td>:host</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: Vaadin.RadioButtonElement#1.0.0-alpha8",
        "Flow#1.0-SNAPSHOT" })
@Tag("vaadin-radio-button")
@HtmlImport("frontend://bower_components/vaadin-radio-button/vaadin-radio-button.html")
public class GeneratedVaadinRadioButton<R extends GeneratedVaadinRadioButton<R>>
        extends Component
        implements ComponentSupplier<R>, HasStyle, Focusable<R>, HasComponents {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Specify that this control should have input focus when the page loads.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code autofocus} property from the webcomponent
     */
    public boolean isAutofocus() {
        return getElement().getProperty("autofocus", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Specify that this control should have input focus when the page loads.
     * </p>
     * 
     * @param autofocus
     *            the boolean value to set
     */
    public void setAutofocus(boolean autofocus) {
        getElement().setProperty("autofocus", autofocus);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the user cannot interact with this element.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code disabled} property from the webcomponent
     */
    public boolean isDisabled() {
        return getElement().getProperty("disabled", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the user cannot interact with this element.
     * </p>
     * 
     * @param disabled
     *            the boolean value to set
     */
    public void setDisabled(boolean disabled) {
        getElement().setProperty("disabled", disabled);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code name} property from the webcomponent
     */
    public String getName() {
        return getElement().getProperty("name");
    }

    /**
     * @param name
     *            the String value to set
     */
    public void setName(java.lang.String name) {
        getElement().setProperty("name", name == null ? "" : name);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the radio button is checked.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'checked-changed' event happens.
     * </p>
     * 
     * @return the {@code checked} property from the webcomponent
     */
    @Synchronize(property = "checked", value = "checked-changed")
    public boolean isChecked() {
        return getElement().getProperty("checked", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the radio button is checked.
     * </p>
     * 
     * @param checked
     *            the boolean value to set
     */
    public void setChecked(boolean checked) {
        getElement().setProperty("checked", checked);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The value for this element.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code value} property from the webcomponent
     */
    public String getValue() {
        return getElement().getProperty("value");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The value for this element.
     * </p>
     * 
     * @param value
     *            the String value to set
     */
    public void setValue(java.lang.String value) {
        getElement().setProperty("value", value == null ? "" : value);
    }

    @DomEvent("checked-changed")
    public static class CheckedChangeEvent<R extends GeneratedVaadinRadioButton<R>>
            extends ComponentEvent<R> {
        public CheckedChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code checked-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     */
    public Registration addCheckedChangeListener(
            ComponentEventListener<CheckedChangeEvent<R>> listener) {
        return addListener(CheckedChangeEvent.class,
                (ComponentEventListener) listener);
    }

    /**
     * Adds the given components as children of this component.
     * 
     * @param components
     *            the components to add
     * @see HasComponents#add(Component...)
     */
    public GeneratedVaadinRadioButton(com.vaadin.ui.Component... components) {
        add(components);
    }

    /**
     * Default constructor.
     */
    public GeneratedVaadinRadioButton() {
    }
}