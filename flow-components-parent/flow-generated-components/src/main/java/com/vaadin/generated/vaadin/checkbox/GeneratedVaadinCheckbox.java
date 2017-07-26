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
package com.vaadin.generated.vaadin.checkbox;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentSupplier;
import com.vaadin.ui.HasStyle;
import com.vaadin.ui.Focusable;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Synchronize;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;
import com.vaadin.ui.HasComponents;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code <vaadin-checkbox>} is a Polymer element for customized checkboxes.
 * 
 * {@code }`html <vaadin-checkbox> Make my profile visible </vaadin-checkbox>
 * {@code }`
 * 
 * ### Styling
 * 
 * The following shadow DOM parts are exposed for styling:
 * 
 * Part name | Description ------------------|---------------- {@code wrapper} |
 * The {@code <label>} element which wrapps the checkbox and [part="label"]
 * {@code native-checkbox} | The {@code <input type="checkbox">} element
 * {@code checkbox} | The {@code <span>} element for a custom graphical check
 * {@code label} | The {@code <span>} element for slotted text/HTML label
 * 
 * The following attributes are exposed for styling:
 * 
 * Attribute | Description -------------|------------ {@code active} | Set when
 * the checkbox is pressed down, either with mouse, touch or the keyboard.
 * {@code disabled} | Set when the checkbox is disabled. {@code focus-ring} |
 * Set when the checkbox is focused using the keyboard. {@code focused} | Set
 * when the checkbox is focused.
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.15-SNAPSHOT",
		"WebComponent: Vaadin.CheckboxElement#null", "Flow#0.1.15-SNAPSHOT"})
@Tag("vaadin-checkbox")
@HtmlImport("frontend://bower_components/vaadin-checkbox/vaadin-checkbox.html")
public class GeneratedVaadinCheckbox<R extends GeneratedVaadinCheckbox<R>>
		extends
			Component
		implements
			ComponentSupplier<R>,
			HasStyle,
			Focusable<R>,
			HasComponents {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specify that this control should have input focus when the page loads.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isAutofocus() {
		return getElement().getProperty("autofocus", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specify that this control should have input focus when the page loads.
	 * 
	 * @param autofocus
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setAutofocus(boolean autofocus) {
		getElement().setProperty("autofocus", autofocus);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the element currently has focus.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isFocused() {
		return getElement().getProperty("focused", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the user cannot interact with this element.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isDisabled() {
		return getElement().getProperty("disabled", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the user cannot interact with this element.
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
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the checkbox is checked.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'checked-changed' event happens.
	 */
	@Synchronize(property = "checked", value = "checked-changed")
	public boolean isChecked() {
		return getElement().getProperty("checked", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the checkbox is checked.
	 * 
	 * @param checked
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setChecked(boolean checked) {
		getElement().setProperty("checked", checked);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Indeterminate state of the checkbox when it's neither checked nor
	 * unchecked, but undetermined.
	 * https://developer.mozilla.org/en-US/docs/Web/
	 * HTML/Element/input/checkbox#Indeterminate_state_checkboxes
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'indeterminate-changed' event happens.
	 */
	@Synchronize(property = "indeterminate", value = "indeterminate-changed")
	public boolean isIndeterminate() {
		return getElement().getProperty("indeterminate", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Indeterminate state of the checkbox when it's neither checked nor
	 * unchecked, but undetermined.
	 * https://developer.mozilla.org/en-US/docs/Web/
	 * HTML/Element/input/checkbox#Indeterminate_state_checkboxes
	 * 
	 * @param indeterminate
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setIndeterminate(boolean indeterminate) {
		getElement().setProperty("indeterminate", indeterminate);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of the control, which is submitted with the form data.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getName() {
		return getElement().getProperty("name");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of the control, which is submitted with the form data.
	 * 
	 * @param name
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setName(java.lang.String name) {
		getElement().setProperty("name", name == null ? "" : name);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The value given to the data submitted with the checkbox's name to the
	 * server when the control is inside a form.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getPostValue() {
		return getElement().getProperty("value");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The value given to the data submitted with the checkbox's name to the
	 * server when the control is inside a form.
	 * 
	 * @param postValue
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setPostValue(java.lang.String postValue) {
		getElement().setProperty("value", postValue == null ? "" : postValue);
		return get();
	}

	public void connectedCallback() {
		getElement().callFunction("connectedCallback");
	}

	public void disconnectedCallback() {
		getElement().callFunction("disconnectedCallback");
	}

	@DomEvent("checked-changed")
	public static class CheckedChangeEvent
			extends
				ComponentEvent<GeneratedVaadinCheckbox> {
		public CheckedChangeEvent(GeneratedVaadinCheckbox source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addCheckedChangeListener(
			ComponentEventListener<CheckedChangeEvent> listener) {
		return addListener(CheckedChangeEvent.class, listener);
	}

	@DomEvent("indeterminate-changed")
	public static class IndeterminateChangeEvent
			extends
				ComponentEvent<GeneratedVaadinCheckbox> {
		public IndeterminateChangeEvent(GeneratedVaadinCheckbox source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIndeterminateChangeListener(
			ComponentEventListener<IndeterminateChangeEvent> listener) {
		return addListener(IndeterminateChangeEvent.class, listener);
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