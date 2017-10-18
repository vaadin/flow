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
package com.vaadin.ui.textfield;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
import com.vaadin.ui.common.Focusable;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.event.Synchronize;
import com.vaadin.ui.common.HasValue;
import java.util.Objects;
import com.vaadin.ui.common.NotSupported;
import com.vaadin.ui.event.DomEvent;
import com.vaadin.ui.event.ComponentEvent;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-text-field>} is a Polymer 2 element for text field control in
 * forms.
 * </p>
 * <p>
 * {@code }
 * <code>html &lt;vaadin-text-field label=&quot;First Name&quot;&gt; &lt;/vaadin-text-field&gt; {@code }</code>
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
 * <td>{@code label}</td>
 * <td>The label element</td>
 * </tr>
 * <tr>
 * <td>{@code value}</td>
 * <td>The input element</td>
 * </tr>
 * <tr>
 * <td>{@code error-message}</td>
 * <td>The error message element</td>
 * </tr>
 * <tr>
 * <td>{@code input-field}</td>
 * <td>The element that wraps prefix, value and suffix</td>
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
 * <td>Set to a disabled text field</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code has-value}</td>
 * <td>Set when the element has a value</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code invalid}</td>
 * <td>Set when the element is invalid</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code focused}</td>
 * <td>Set when the element is focused</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code focus-ring}</td>
 * <td>Set when the element is keyboard focused</td>
 * <td>:host</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({"Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
		"WebComponent: Vaadin.TextFieldElement#1.1.0", "Flow#1.0-SNAPSHOT"})
@Tag("vaadin-text-field")
@HtmlImport("frontend://bower_components/vaadin-text-field/vaadin-text-field.html")
public class GeneratedVaadinTextField<R extends GeneratedVaadinTextField<R>>
		extends
			Component
		implements
			ComponentSupplier<R>,
			HasStyle,
			Focusable<R>,
			HasValue<R, String> {

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
	 * Whether the value of the control can be automatically completed by the
	 * browser. List of available options at:
	 * https://developer.mozilla.org/en/docs
	 * /Web/HTML/Element/input#attr-autocomplete
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code autocomplete} property from the webcomponent
	 */
	public String getAutocomplete() {
		return getElement().getProperty("autocomplete");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Whether the value of the control can be automatically completed by the
	 * browser. List of available options at:
	 * https://developer.mozilla.org/en/docs
	 * /Web/HTML/Element/input#attr-autocomplete
	 * </p>
	 * 
	 * @param autocomplete
	 *            the String value to set
	 */
	public void setAutocomplete(java.lang.String autocomplete) {
		getElement().setProperty("autocomplete",
				autocomplete == null ? "" : autocomplete);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * This is a property supported by Safari that is used to control whether
	 * autocorrection should be enabled when the user is entering/editing the
	 * text. Possible values are: on: Enable autocorrection. off: Disable
	 * autocorrection.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code autocorrect} property from the webcomponent
	 */
	public String getAutocorrect() {
		return getElement().getProperty("autocorrect");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * This is a property supported by Safari that is used to control whether
	 * autocorrection should be enabled when the user is entering/editing the
	 * text. Possible values are: on: Enable autocorrection. off: Disable
	 * autocorrection.
	 * </p>
	 * 
	 * @param autocorrect
	 *            the String value to set
	 */
	public void setAutocorrect(java.lang.String autocorrect) {
		getElement().setProperty("autocorrect",
				autocorrect == null ? "" : autocorrect);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Error to show when the input value is invalid.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code errorMessage} property from the webcomponent
	 */
	public String getErrorMessage() {
		return getElement().getProperty("errorMessage");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Error to show when the input value is invalid.
	 * </p>
	 * 
	 * @param errorMessage
	 *            the String value to set
	 */
	public void setErrorMessage(java.lang.String errorMessage) {
		getElement().setProperty("errorMessage",
				errorMessage == null ? "" : errorMessage);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * String used for the label element.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code label} property from the webcomponent
	 */
	public String getLabel() {
		return getElement().getProperty("label");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * String used for the label element.
	 * </p>
	 * 
	 * @param label
	 *            the String value to set
	 */
	public void setLabel(java.lang.String label) {
		getElement().setProperty("label", label == null ? "" : label);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Identifies a list of pre-defined options to suggest to the user. The
	 * value must be the id of a <datalist> element in the same document.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code list} property from the webcomponent
	 */
	public String getList() {
		return getElement().getProperty("list");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Identifies a list of pre-defined options to suggest to the user. The
	 * value must be the id of a <datalist> element in the same document.
	 * </p>
	 * 
	 * @param list
	 *            the String value to set
	 */
	public void setList(java.lang.String list) {
		getElement().setProperty("list", list == null ? "" : list);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Maximum number of characters (in Unicode code points) that the user can
	 * enter.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code maxlength} property from the webcomponent
	 */
	public double getMaxlength() {
		return getElement().getProperty("maxlength", 0.0);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Maximum number of characters (in Unicode code points) that the user can
	 * enter.
	 * </p>
	 * 
	 * @param maxlength
	 *            the double value to set
	 */
	public void setMaxlength(double maxlength) {
		getElement().setProperty("maxlength", maxlength);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Minimum number of characters (in Unicode code points) that the user can
	 * enter.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code minlength} property from the webcomponent
	 */
	public double getMinlength() {
		return getElement().getProperty("minlength", 0.0);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Minimum number of characters (in Unicode code points) that the user can
	 * enter.
	 * </p>
	 * 
	 * @param minlength
	 *            the double value to set
	 */
	public void setMinlength(double minlength) {
		getElement().setProperty("minlength", minlength);
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
	 * A regular expression that the value is checked against. The pattern must
	 * match the entire value, not just some subset.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code pattern} property from the webcomponent
	 */
	public String getPattern() {
		return getElement().getProperty("pattern");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * A regular expression that the value is checked against. The pattern must
	 * match the entire value, not just some subset.
	 * </p>
	 * 
	 * @param pattern
	 *            the String value to set
	 */
	public void setPattern(java.lang.String pattern) {
		getElement().setProperty("pattern", pattern == null ? "" : pattern);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * A hint to the user of what can be entered in the control.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code placeholder} property from the webcomponent
	 */
	public String getPlaceholder() {
		return getElement().getProperty("placeholder");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * A hint to the user of what can be entered in the control.
	 * </p>
	 * 
	 * @param placeholder
	 *            the String value to set
	 */
	public void setPlaceholder(java.lang.String placeholder) {
		getElement().setProperty("placeholder",
				placeholder == null ? "" : placeholder);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * This attribute indicates that the user cannot modify the value of the
	 * control.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code readonly} property from the webcomponent
	 */
	public boolean isReadonly() {
		return getElement().getProperty("readonly", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * This attribute indicates that the user cannot modify the value of the
	 * control.
	 * </p>
	 * 
	 * @param readonly
	 *            the boolean value to set
	 */
	public void setReadonly(boolean readonly) {
		getElement().setProperty("readonly", readonly);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Specifies that the user must fill in a value.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code required} property from the webcomponent
	 */
	public boolean isRequired() {
		return getElement().getProperty("required", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Specifies that the user must fill in a value.
	 * </p>
	 * 
	 * @param required
	 *            the boolean value to set
	 */
	public void setRequired(boolean required) {
		getElement().setProperty("required", required);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Message to show to the user when validation fails.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code title} property from the webcomponent
	 */
	public String getTitle() {
		return getElement().getProperty("title");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Message to show to the user when validation fails.
	 * </p>
	 * 
	 * @param title
	 *            the String value to set
	 */
	public void setTitle(java.lang.String title) {
		getElement().setProperty("title", title == null ? "" : title);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The initial value of the control. It can be used for two-way data
	 * binding.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'value-changed' event happens.
	 * </p>
	 * 
	 * @return the {@code value} property from the webcomponent
	 */
	@Synchronize(property = "value", value = "value-changed")
	@Override
	public String getValue() {
		return getElement().getProperty("value");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The initial value of the control. It can be used for two-way data
	 * binding.
	 * </p>
	 * 
	 * @param value
	 *            the String value to set
	 */
	@Override
	public void setValue(java.lang.String value) {
		if (!Objects.equals(value, getValue())) {
			getElement().setProperty("value", value == null ? "" : value);
		}
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * This property is set to true when the control value is invalid.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'invalid-changed' event happens.
	 * </p>
	 * 
	 * @return the {@code invalid} property from the webcomponent
	 */
	@Synchronize(property = "invalid", value = "invalid-changed")
	public boolean isInvalid() {
		return getElement().getProperty("invalid", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * This property is set to true when the control value is invalid.
	 * </p>
	 * 
	 * @param invalid
	 *            the boolean value to set
	 */
	public void setInvalid(boolean invalid) {
		getElement().setProperty("invalid", invalid);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * A read-only property indicating whether this input has a non empty value.
	 * It can be used for example in styling of the component.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code hasValue} property from the webcomponent
	 */
	public boolean hasValue() {
		return getElement().getProperty("hasValue", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * When set to true, user is prevented from typing a value that conflicts
	 * with the given {@code pattern}.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code preventInvalidInput} property from the webcomponent
	 */
	public boolean isPreventInvalidInput() {
		return getElement().getProperty("preventInvalidInput", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * When set to true, user is prevented from typing a value that conflicts
	 * with the given {@code pattern}.
	 * </p>
	 * 
	 * @param preventInvalidInput
	 *            the boolean value to set
	 */
	public void setPreventInvalidInput(boolean preventInvalidInput) {
		getElement().setProperty("preventInvalidInput", preventInvalidInput);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Returns true if {@code value} is valid. {@code <iron-form>} uses this to
	 * check the validity or all its elements.
	 * </p>
	 * <p>
	 * This function is not supported by Flow because it returns a
	 * <code>boolean</code>. Functions with return types different than void are
	 * not supported at this moment.
	 */
	@NotSupported
	protected void validate() {
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Returns true if the current input value satisfies all constraints (if
	 * any)
	 * </p>
	 */
	public void checkValidity() {
		getElement().callFunction("checkValidity");
	}

	@DomEvent("iron-form-element-register")
	public static class IronFormElementRegisterEvent<R extends GeneratedVaadinTextField<R>>
			extends
				ComponentEvent<R> {
		public IronFormElementRegisterEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronFormElementRegisterListener(
			ComponentEventListener<IronFormElementRegisterEvent<R>> listener) {
		return addListener(IronFormElementRegisterEvent.class,
				(ComponentEventListener) listener);
	}

	@DomEvent("iron-form-element-unregister")
	public static class IronFormElementUnregisterEvent<R extends GeneratedVaadinTextField<R>>
			extends
				ComponentEvent<R> {
		public IronFormElementUnregisterEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronFormElementUnregisterListener(
			ComponentEventListener<IronFormElementUnregisterEvent<R>> listener) {
		return addListener(IronFormElementUnregisterEvent.class,
				(ComponentEventListener) listener);
	}

	@DomEvent("invalid-changed")
	public static class InvalidChangeEvent<R extends GeneratedVaadinTextField<R>>
			extends
				ComponentEvent<R> {
		public InvalidChangeEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addInvalidChangeListener(
			ComponentEventListener<InvalidChangeEvent<R>> listener) {
		return addListener(InvalidChangeEvent.class,
				(ComponentEventListener) listener);
	}

	@DomEvent("has-value-changed")
	public static class HasValueChangeEvent<R extends GeneratedVaadinTextField<R>>
			extends
				ComponentEvent<R> {
		public HasValueChangeEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addHasValueChangeListener(
			ComponentEventListener<HasValueChangeEvent<R>> listener) {
		return addListener(HasValueChangeEvent.class,
				(ComponentEventListener) listener);
	}

	/**
	 * Adds the given components as children of this component at the slot
	 * 'prefix'.
	 * 
	 * @param components
	 *            The components to add.
	 * @see <a
	 *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
	 *      page about slots</a>
	 * @see <a
	 *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
	 *      website about slots</a>
	 * @return this instance, for method chaining
	 */
	public R addToPrefix(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			component.getElement().setAttribute("slot", "prefix");
			getElement().appendChild(component.getElement());
		}
		return get();
	}

	/**
	 * Adds the given components as children of this component at the slot
	 * 'suffix'.
	 * 
	 * @param components
	 *            The components to add.
	 * @see <a
	 *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
	 *      page about slots</a>
	 * @see <a
	 *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
	 *      website about slots</a>
	 * @return this instance, for method chaining
	 */
	public R addToSuffix(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			component.getElement().setAttribute("slot", "suffix");
			getElement().appendChild(component.getElement());
		}
		return get();
	}

	/**
	 * Removes the given child components from this component.
	 * 
	 * @param components
	 *            The components to remove.
	 * @throws IllegalArgumentException
	 *             if any of the components is not a child of this component.
	 */
	public void remove(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			if (getElement().equals(component.getElement().getParent())) {
				component.getElement().removeAttribute("slot");
				getElement().removeChild(component.getElement());
			} else {
				throw new IllegalArgumentException("The given component ("
						+ component + ") is not a child of this component");
			}
		}
	}

	/**
	 * Removes all contents from this component, this includes child components,
	 * text content as well as child elements that have been added directly to
	 * this component using the {@link Element} API.
	 */
	public void removeAll() {
		getElement().getChildren().forEach(
				child -> child.removeAttribute("slot"));
		getElement().removeAllChildren();
	}
}