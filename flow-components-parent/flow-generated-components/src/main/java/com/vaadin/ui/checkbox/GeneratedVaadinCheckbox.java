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
package com.vaadin.ui.checkbox;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
import com.vaadin.ui.common.Focusable;
import com.vaadin.ui.common.HasClickListeners;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.event.Synchronize;
import com.vaadin.ui.common.HasValue;
import java.util.Objects;
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
 * {@code <vaadin-checkbox>} is a Polymer 2 element for customized checkboxes.
 * </p>
 * <p>
 * {@code }
 * <code>html &lt;vaadin-checkbox&gt; Make my profile visible &lt;/vaadin-checkbox&gt; {@code }</code>
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
 * <td>The {@code <label>} element which wraps the checkbox and
 * [part=&quot;label&quot;]</td>
 * </tr>
 * <tr>
 * <td>{@code native-checkbox}</td>
 * <td>The {@code <input type="checkbox">} element</td>
 * </tr>
 * <tr>
 * <td>{@code checkbox}</td>
 * <td>The {@code <span>} element for a custom graphical check</td>
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
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code active}</td>
 * <td>Set when the checkbox is pressed down, either with mouse, touch or the
 * keyboard.</td>
 * </tr>
 * <tr>
 * <td>{@code disabled}</td>
 * <td>Set when the checkbox is disabled.</td>
 * </tr>
 * <tr>
 * <td>{@code focus-ring}</td>
 * <td>Set when the checkbox is focused using the keyboard.</td>
 * </tr>
 * <tr>
 * <td>{@code focused}</td>
 * <td>Set when the checkbox is focused.</td>
 * </tr>
 * <tr>
 * <td>{@code indeterminate}</td>
 * <td>Set when the checkbox is in indeterminate mode.</td>
 * </tr>
 * <tr>
 * <td>{@code checked}</td>
 * <td>Set when the checkbox is checked.</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({"Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
		"WebComponent: Vaadin.CheckboxElement#1.0.0", "Flow#1.0-SNAPSHOT"})
@Tag("vaadin-checkbox")
@HtmlImport("frontend://bower_components/vaadin-checkbox/vaadin-checkbox.html")
public class GeneratedVaadinCheckbox<R extends GeneratedVaadinCheckbox<R>>
		extends
			Component
		implements
			ComponentSupplier<R>,
			HasStyle,
			Focusable<R>,
			HasClickListeners<R>,
			HasValue<R, Boolean>,
			HasComponents {

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
	 * If true, the element currently has focus. This property is @deprecated
	 * and will not be accessible in the next major version of the component.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code focused} property from the webcomponent
	 */
	public boolean isFocused() {
		return getElement().getProperty("focused", false);
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
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * True if the checkbox is checked.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'checked-changed' event happens.
	 * </p>
	 * 
	 * @return the {@code checked} property from the webcomponent
	 */
	@Synchronize(property = "checked", value = "checked-changed")
	@Override
	public Boolean getValue() {
		return getElement().getProperty("checked", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * True if the checkbox is checked.
	 * </p>
	 * 
	 * @param value
	 *            the boolean value to set
	 */
	@Override
	public void setValue(java.lang.Boolean value) {
		Objects.requireNonNull(value,
				"GeneratedVaadinCheckbox value must not be null");
		if (!Objects.equals(value, getValue())) {
			getElement().setProperty("checked", value);
		}
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Indeterminate state of the checkbox when it's neither checked nor
	 * unchecked, but undetermined.
	 * https://developer.mozilla.org/en-US/docs/Web/
	 * HTML/Element/input/checkbox#Indeterminate_state_checkboxes
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'indeterminate-changed' event happens.
	 * </p>
	 * 
	 * @return the {@code indeterminate} property from the webcomponent
	 */
	@Synchronize(property = "indeterminate", value = "indeterminate-changed")
	public boolean isIndeterminate() {
		return getElement().getProperty("indeterminate", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Indeterminate state of the checkbox when it's neither checked nor
	 * unchecked, but undetermined.
	 * https://developer.mozilla.org/en-US/docs/Web/
	 * HTML/Element/input/checkbox#Indeterminate_state_checkboxes
	 * </p>
	 * 
	 * @param indeterminate
	 *            the boolean value to set
	 */
	public void setIndeterminate(boolean indeterminate) {
		getElement().setProperty("indeterminate", indeterminate);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The name of the control, which is submitted with the form data.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code name} property from the webcomponent
	 */
	public String getName() {
		return getElement().getProperty("name");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The name of the control, which is submitted with the form data.
	 * </p>
	 * 
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
	 * The value given to the data submitted with the checkbox's name to the
	 * server when the control is inside a form.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code value} property from the webcomponent
	 */
	public String getPostValue() {
		return getElement().getProperty("value");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The value given to the data submitted with the checkbox's name to the
	 * server when the control is inside a form.
	 * </p>
	 * 
	 * @param postValue
	 *            the String value to set
	 */
	public void setPostValue(java.lang.String postValue) {
		getElement().setProperty("value", postValue == null ? "" : postValue);
	}

	@Override
	public String getClientValuePropertyName() {
		return "checked";
	}

	@DomEvent("indeterminate-changed")
	public static class IndeterminateChangeEvent<R extends GeneratedVaadinCheckbox<R>>
			extends
				ComponentEvent<R> {
		public IndeterminateChangeEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIndeterminateChangeListener(
			ComponentEventListener<IndeterminateChangeEvent<R>> listener) {
		return addListener(IndeterminateChangeEvent.class,
				(ComponentEventListener) listener);
	}

	/**
	 * Adds the given components as children of this component.
	 * 
	 * @param components
	 *            the components to add
	 * @see HasComponents#add(Component...)
	 */
	public GeneratedVaadinCheckbox(com.vaadin.ui.Component... components) {
		add(components);
	}

	/**
	 * Default constructor.
	 */
	public GeneratedVaadinCheckbox() {
	}
}