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
package com.vaadin.ui.combobox;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import com.vaadin.ui.common.NotSupported;
import com.vaadin.ui.event.EventData;
import com.vaadin.ui.event.DomEvent;
import com.vaadin.ui.event.ComponentEvent;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-combo-box>} is a combo box element combining a dropdown list
 * with an input field for filtering the list of items. If you want to replace
 * the default input field with a custom implementation, you should use the <a
 * href="#/elements/vaadin-combo-box-light">{@code <vaadin-combo-box-light>}</a>
 * element.
 * </p>
 * <p>
 * Items in the dropdown list must be provided as a list of {@code String}
 * values. Defining the items is done using the {@code items} property, which
 * can be assigned with data-binding, using an attribute or directly with the
 * JavaScript property.
 * </p>
 * <p>
 * {@code }
 * <code>html &lt;vaadin-combo-box label=&quot;Fruit&quot; items=&quot;[[data]]&quot;&gt; &lt;/vaadin-combo-box&gt; {@code }</code>
 * </p>
 * <p>
 * {@code }
 * <code>js combobox.items = ['apple', 'orange', 'banana']; {@code }</code>
 * </p>
 * <p>
 * When the selected {@code value} is changed, a {@code value-changed} event is
 * triggered.
 * </p>
 * <p>
 * This element can be used within an {@code iron-form}.
 * </p>
 * <h3>Item Template</h3>
 * <p>
 * {@code <vaadin-combo-box>} supports using custom item template provided in
 * the light DOM:
 * </p>
 * <p>
 * {@code }
 * <code>html &lt;vaadin-combo-box items='[{&quot;label&quot;: &quot;Hydrogen&quot;, &quot;value&quot;: &quot;H&quot;}]'&gt; &lt;template&gt; [[index]]: [[item.label]] &lt;b&gt;[[item.value]&lt;/b&gt; &lt;/template&gt; &lt;/vaadin-combo-box&gt; {@code }</code>
 * </p>
 * <p>
 * The following properties are available for item template bindings:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Property name</th>
 * <th>Type</th>
 * <th>Description</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code index}</td>
 * <td>Number</td>
 * <td>Index of the item in the {@code items} array</td>
 * </tr>
 * <tr>
 * <td>{@code item}</td>
 * <td>String or Object</td>
 * <td>The item reference</td>
 * </tr>
 * <tr>
 * <td>{@code selected}</td>
 * <td>Boolean</td>
 * <td>True when item is selected</td>
 * </tr>
 * <tr>
 * <td>{@code focused}</td>
 * <td>Boolean</td>
 * <td>True when item is focused</td>
 * </tr>
 * </tbody>
 * </table>
 * <h3>Styling</h3>
 * <p>
 * <a href=
 * "https://cdn.vaadin.com/vaadin-valo-theme/0.3.1/demo/customization.html"
 * >Generic styling/theming documentation</a>
 * </p>
 * <p>
 * The following custom properties are available for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Custom property</th>
 * <th>Description</th>
 * <th>Default</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code --vaadin-combo-box-overlay-max-height}</td>
 * <td>Property that determines the max height of overlay</td>
 * <td>{@code 65vh}</td>
 * </tr>
 * </tbody>
 * </table>
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
 * <td>{@code text-field}</td>
 * <td>The text field</td>
 * </tr>
 * <tr>
 * <td>{@code clear-button}</td>
 * <td>The clear button</td>
 * </tr>
 * <tr>
 * <td>{@code toggle-button}</td>
 * <td>The toggle button</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * See <a href=
 * "https://github.com/vaadin/vaadin-overlay/blob/master/vaadin-overlay.html">
 * {@code <vaadin-overlay>} documentation</a> for
 * {@code <vaadin-combo-box-overlay>} parts.
 * </p>
 * <p>
 * See <a href=
 * "https://vaadin.com/elements/vaadin-text-field/html-api/elements/Vaadin.TextFieldElement"
 * >{@code <vaadin-text-field>} documentation</a> for the text field parts.
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
 * <td>{@code opened}</td>
 * <td>Set when the combo box dropdown is open</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code disabled}</td>
 * <td>Set to a disabled combo box</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code readonly}</td>
 * <td>Set to a read only combo box</td>
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
 * <tr>
 * <td>{@code loading}</td>
 * <td>Set when new items are expected</td>
 * <td>:host</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({"Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
		"WebComponent: Vaadin.ComboBoxElement#3.0.0", "Flow#1.0-SNAPSHOT"})
@Tag("vaadin-combo-box")
@HtmlImport("frontend://bower_components/vaadin-combo-box/vaadin-combo-box.html")
public class GeneratedVaadinComboBox<R extends GeneratedVaadinComboBox<R>>
		extends
			Component implements ComponentSupplier<R>, HasStyle {

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * True if the dropdown is open, false otherwise.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code opened} property from the webcomponent
	 */
	public boolean isOpened() {
		return getElement().getProperty("opened", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * True if the dropdown is open, false otherwise.
	 * </p>
	 * 
	 * @param opened
	 *            the boolean value to set
	 */
	public void setOpened(boolean opened) {
		getElement().setProperty("opened", opened);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set to true to disable this input.
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
	 * Set to true to disable this input.
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
	 * @return the {@code readonly} property from the webcomponent
	 */
	public boolean isReadonly() {
		return getElement().getProperty("readonly", false);
	}

	/**
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
	 * A full set of items to filter the visible options from. The items can be
	 * of either {@code String} or {@code Object} type.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code items} property from the webcomponent
	 */
	protected JsonArray protectedGetItems() {
		return (JsonArray) getElement().getPropertyRaw("items");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * A full set of items to filter the visible options from. The items can be
	 * of either {@code String} or {@code Object} type.
	 * </p>
	 * 
	 * @param items
	 *            the JsonArray value to set
	 */
	protected void setItems(elemental.json.JsonArray items) {
		getElement().setPropertyJson("items", items);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * If {@code true}, the user can input a value that is not present in the
	 * items list. {@code value} property will be set to the input value in this
	 * case. Also, when {@code value} is set programmatically, the input value
	 * will be set to reflect that value.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code allowCustomValue} property from the webcomponent
	 */
	public boolean isAllowCustomValue() {
		return getElement().getProperty("allowCustomValue", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * If {@code true}, the user can input a value that is not present in the
	 * items list. {@code value} property will be set to the input value in this
	 * case. Also, when {@code value} is set programmatically, the input value
	 * will be set to reflect that value.
	 * </p>
	 * 
	 * @param allowCustomValue
	 *            the boolean value to set
	 */
	public void setAllowCustomValue(boolean allowCustomValue) {
		getElement().setProperty("allowCustomValue", allowCustomValue);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * A subset of items, filtered based on the user input. Filtered items can
	 * be assigned directly to omit the internal filtering functionality. The
	 * items can be of either {@code String} or {@code Object} type.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code filteredItems} property from the webcomponent
	 */
	protected JsonArray protectedGetFilteredItems() {
		return (JsonArray) getElement().getPropertyRaw("filteredItems");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * A subset of items, filtered based on the user input. Filtered items can
	 * be assigned directly to omit the internal filtering functionality. The
	 * items can be of either {@code String} or {@code Object} type.
	 * </p>
	 * 
	 * @param filteredItems
	 *            the JsonArray value to set
	 */
	protected void setFilteredItems(elemental.json.JsonArray filteredItems) {
		getElement().setPropertyJson("filteredItems", filteredItems);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * A read-only property indicating whether this combo box has a value
	 * selected or not. It can be used for example in styling of the component.
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
	 * When set to {@code true}, &quot;loading&quot; attibute is added to host
	 * and the overlay element.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code loading} property from the webcomponent
	 */
	public boolean isLoading() {
		return getElement().getProperty("loading", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * When set to {@code true}, &quot;loading&quot; attibute is added to host
	 * and the overlay element.
	 * </p>
	 * 
	 * @param loading
	 *            the boolean value to set
	 */
	public void setLoading(boolean loading) {
		getElement().setProperty("loading", loading);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Filtering string the user has typed into the input field.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code filter} property from the webcomponent
	 */
	public String getFilter() {
		return getElement().getProperty("filter");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Filtering string the user has typed into the input field.
	 * </p>
	 * 
	 * @param filter
	 *            the String value to set
	 */
	public void setFilter(java.lang.String filter) {
		getElement().setProperty("filter", filter == null ? "" : filter);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set to true to mark the input as required.
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
	 * Set to true to mark the input as required.
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
	 * The name of this element.
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
	 * The name of this element.
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
	 * Set to true if the value is invalid.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code invalid} property from the webcomponent
	 */
	public boolean isInvalid() {
		return getElement().getProperty("invalid", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set to true if the value is invalid.
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
	 * The label for this element.
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
	 * The label for this element.
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
	 * Set to true to disable the floating label.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code noLabelFloat} property from the webcomponent
	 */
	public boolean isNoLabelFloat() {
		return getElement().getProperty("noLabelFloat", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set to true to disable the floating label.
	 * </p>
	 * 
	 * @param noLabelFloat
	 *            the boolean value to set
	 */
	public void setNoLabelFloat(boolean noLabelFloat) {
		getElement().setProperty("noLabelFloat", noLabelFloat);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set to true to always float the label.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code alwaysFloatLabel} property from the webcomponent
	 */
	public boolean isAlwaysFloatLabel() {
		return getElement().getProperty("alwaysFloatLabel", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set to true to always float the label.
	 * </p>
	 * 
	 * @param alwaysFloatLabel
	 *            the boolean value to set
	 */
	public void setAlwaysFloatLabel(boolean alwaysFloatLabel) {
		getElement().setProperty("alwaysFloatLabel", alwaysFloatLabel);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set to true to prevent the user from entering invalid input.
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
	 * Set to true to prevent the user from entering invalid input.
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
	 * Set this to specify the pattern allowed by {@code preventInvalidInput}.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code allowedPattern} property from the webcomponent
	 */
	public String getAllowedPattern() {
		return getElement().getProperty("allowedPattern");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set this to specify the pattern allowed by {@code preventInvalidInput}.
	 * </p>
	 * 
	 * @param allowedPattern
	 *            the String value to set
	 */
	public void setAllowedPattern(java.lang.String allowedPattern) {
		getElement().setProperty("allowedPattern",
				allowedPattern == null ? "" : allowedPattern);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * A pattern to validate the {@code input} with.
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
	 * A pattern to validate the {@code input} with.
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
	 * The error message to display when the input is invalid.
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
	 * The error message to display when the input is invalid.
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
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * 
	 * @return the {@code autofocus} property from the webcomponent
	 */
	public boolean isAutofocus() {
		return getElement().getProperty("autofocus", false);
	}

	/**
	 * @param autofocus
	 *            the boolean value to set
	 */
	public void setAutofocus(boolean autofocus) {
		getElement().setProperty("autofocus", autofocus);
	}

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * 
	 * @return the {@code inputmode} property from the webcomponent
	 */
	public String getInputmode() {
		return getElement().getProperty("inputmode");
	}

	/**
	 * @param inputmode
	 *            the String value to set
	 */
	public void setInputmode(java.lang.String inputmode) {
		getElement().setProperty("inputmode",
				inputmode == null ? "" : inputmode);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * A placeholder string in addition to the label. If this is set, the label
	 * will always float.
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
	 * A placeholder string in addition to the label. If this is set, the label
	 * will always float.
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
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * 
	 * @return the {@code size} property from the webcomponent
	 */
	public double getSize() {
		return getElement().getProperty("size", 0.0);
	}

	/**
	 * @param size
	 *            the double value to set
	 */
	public void setSize(double size) {
		getElement().setProperty("size", size);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Opens the dropdown list.
	 * </p>
	 */
	public void open() {
		getElement().callFunction("open");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Closes the dropdown list.
	 * </p>
	 */
	public void close() {
		getElement().callFunction("close");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Reverts back to original value.
	 * </p>
	 */
	public void cancel() {
		getElement().callFunction("cancel");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Returns true if {@code value} is valid, and sets the {@code invalid} flag
	 * appropriatelly.
	 * </p>
	 * <p>
	 * This function is not supported by Flow because it returns a
	 * <code>boolean</code>. Functions with return types different than void are
	 * not supported at this moment.
	 * 
	 * @param value
	 *            Missing documentation!
	 */
	@NotSupported
	protected void validate(JsonObject value) {
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Returns true if the current input value satisfies all constraints (if
	 * any)
	 * </p>
	 * <p>
	 * You can override the {@code checkValidity} method for custom validations.
	 * </p>
	 * 
	 * @param value
	 *            Missing documentation!
	 */
	protected void checkValidity(elemental.json.JsonObject value) {
		getElement().callFunction("checkValidity", value);
	}

	@DomEvent("custom-value-set")
	public static class CustomValueSetEvent<R extends GeneratedVaadinComboBox<R>>
			extends
				ComponentEvent<R> {
		private final String detail;

		public CustomValueSetEvent(R source, boolean fromClient,
				@EventData("event.detail") java.lang.String detail) {
			super(source, fromClient);
			this.detail = detail;
		}

		public String getDetail() {
			return detail;
		}
	}

	public Registration addCustomValueSetListener(
			ComponentEventListener<CustomValueSetEvent<R>> listener) {
		return addListener(CustomValueSetEvent.class,
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