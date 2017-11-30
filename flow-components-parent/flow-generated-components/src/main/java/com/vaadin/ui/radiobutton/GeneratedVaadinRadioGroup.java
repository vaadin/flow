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
import com.vaadin.ui.common.HasStyle;
import com.vaadin.ui.common.ComponentSupplier;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.common.HasComponents;

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
 * <a href=
 * "https://cdn.vaadin.com/vaadin-valo-theme/0.3.1/demo/customization.html"
 * >Generic styling/theming documentation</a>
 * </p>
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
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: Vaadin.RadioGroupElement#1.0.0-alpha8",
        "Flow#1.0-SNAPSHOT" })
@Tag("vaadin-radio-group")
@HtmlImport("frontend://bower_components/vaadin-radio-button/vaadin-radio-group.html")
public class GeneratedVaadinRadioGroup<R extends GeneratedVaadinRadioGroup<R>>
        extends Component
        implements HasStyle, ComponentSupplier<R>, HasComponents {

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
    public boolean isDisabled() {
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
     * @return this instance, for method chaining
     */
    public R setDisabled(boolean disabled) {
        getElement().setProperty("disabled", disabled);
        return get();
    }

    /**
     * Adds the given components as children of this component.
     * 
     * @param components
     *            the components to add
     * @see HasComponents#add(Component...)
     */
    public GeneratedVaadinRadioGroup(Component... components) {
        add(components);
    }

    /**
     * Default constructor.
     */
    public GeneratedVaadinRadioGroup() {
    }
}