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
package com.vaadin.flow.component.radiobutton;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-radio-group>} is a Polymer element for grouping
 * vaadin-radio-buttons.
 * </p>
 * <p>
 * &lt;vaadin-radio-group&gt; &lt;vaadin-radio-button
 * name=&quot;foo&quot;&gt;Foo&lt;/vaadin-radio-button&gt;
 * &lt;vaadin-radio-button
 * name=&quot;bar&quot;&gt;Bar&lt;/vaadin-radio-button&gt;
 * &lt;vaadin-radio-button
 * name=&quot;baz&quot;&gt;Baz&lt;/vaadin-radio-button&gt;
 * &lt;/vaadin-radio-group&gt;
 * </p>
 * <h3>Styling</h3>
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
 * <td>Set when the radio group and its children are disabled.</td>
 * <td>:host</td>
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
        "WebComponent: Vaadin.RadioGroupElement#1.0.0", "Flow#1.0-SNAPSHOT" })
@Tag("vaadin-radio-group")
@HtmlImport("frontend://bower_components/vaadin-radio-button/src/vaadin-radio-group.html")
public abstract class GeneratedVaadinRadioGroup<R extends GeneratedVaadinRadioGroup<R>>
        extends Component implements HasStyle {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The current disabled state of the radio group. True if group and all
     * internal radio buttons are disabled.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code disabled} property from the webcomponent
     */
    protected boolean isDisabledBoolean() {
        return getElement().getProperty("disabled", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The current disabled state of the radio group. True if group and all
     * internal radio buttons are disabled.
     * </p>
     * 
     * @param disabled
     *            the boolean value to set
     */
    protected void setDisabled(boolean disabled) {
        getElement().setProperty("disabled", disabled);
    }

    public static class ValueChangeEvent<R extends GeneratedVaadinRadioGroup<R>>
            extends ComponentEvent<R> {
        public ValueChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code value-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addValueChangeListener(
            ComponentEventListener<ValueChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("value",
                        event -> listener.onComponentEvent(
                                new ValueChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }
}