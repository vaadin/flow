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
package com.vaadin.generated.vaadin.combo.box;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentSupplier;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Synchronize;
import com.vaadin.components.data.HasValue;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import com.vaadin.components.NotSupported;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;
import com.vaadin.annotations.EventData;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-combo-box>} is a combo box element combining a dropdown list
 * with an input field for filtering the list of items. If you want to replace
 * the default input field with a custom implementation, you should use the <a
 * href="#vaadin-combo-box-light">{@code <vaadin-combo-box-light>}</a> element.
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
 * This element is also extended with the {@code IronFormElementBehavior} to
 * enable usage within an {@code iron-form}.
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
 * <p>
 * See the <a href="demo/item-template.html">Item Template Live Demos</a> for
 * more examples.
 * </p>
 * <h3>Styling</h3>
 * <p>
 * There are custom properties and mixins you can use to style the component:
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
 */
@Generated({"Generator: com.vaadin.generator.ComponentGenerator#0.1-SNAPSHOT",
		"WebComponent: vaadin-combo-box#2.0.0", "Flow#0.1-SNAPSHOT"})
@Tag("vaadin-combo-box")
@HtmlImport("frontend://bower_components/vaadin-combo-box/vaadin-combo-box.html")
public class GeneratedVaadinComboBox<R extends GeneratedVaadinComboBox<R>>
		extends
			Component
		implements
			ComponentSupplier<R>,
			HasStyle,
			HasValue<R, String> {

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Name of the validator to use.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 */
	public String getValidator() {
		return getElement().getProperty("validator");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Name of the validator to use.
	 * </p>
	 * 
	 * @param validator
	 *            the String value to set
	 */
	public void setValidator(java.lang.String validator) {
		getElement().setProperty("validator",
				validator == null ? "" : validator);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * True if the last call to {@code validate} is invalid.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'invalid-changed' event happens.
	 * </p>
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
	 * True if the last call to {@code validate} is invalid.
	 * </p>
	 * 
	 * @param invalid
	 *            the boolean value to set
	 */
	public void setInvalid(boolean invalid) {
		getElement().setProperty("invalid", invalid);
	}

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 * The {@code String} value for the selected item of the combo box. Provides
	 * the value for {@code iron-form}.
	 * </p>
	 * <p>
	 * When there’s no item selected, the value is an empty string.
	 * </p>
	 * <p>
	 * Use {@code selectedItem} property to get the raw selected item from the
	 * {@code items} array.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'value-changed' event happens.
	 * </p>
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
	 * The {@code String} value for the selected item of the combo box. Provides
	 * the value for {@code iron-form}.
	 * </p>
	 * <p>
	 * When there’s no item selected, the value is an empty string.
	 * </p>
	 * <p>
	 * Use {@code selectedItem} property to get the raw selected item from the
	 * {@code items} array.
	 * </p>
	 * 
	 * @param value
	 *            the String value to set
	 */
	@Override
	public void setValue(java.lang.String value) {
		getElement().setProperty("value", value == null ? "" : value);
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
	 * True if the dropdown is open, false otherwise.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'opened-changed' event happens.
	 * </p>
	 */
	@Synchronize(property = "opened", value = "opened-changed")
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
	 */
	public boolean hasValue() {
		return getElement().getProperty("hasValue", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * When set to {@code true}, a loading spinner is displayed on top of the
	 * list of options.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 */
	public boolean isLoading() {
		return getElement().getProperty("loading", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * When set to {@code true}, a loading spinner is displayed on top of the
	 * list of options.
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
	 * This property is synchronized automatically from client side when a
	 * 'filter-changed' event happens.
	 * </p>
	 */
	@Synchronize(property = "filter", value = "filter-changed")
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
	 * The selected item from the {@code items} array.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 */
	protected JsonObject protectedGetSelectedItem() {
		return (JsonObject) getElement().getPropertyRaw("selectedItem");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The selected item from the {@code items} array.
	 * </p>
	 * 
	 * @param selectedItem
	 *            the JsonObject value to set
	 */
	protected void setSelectedItem(elemental.json.JsonObject selectedItem) {
		getElement().setPropertyJson("selectedItem", selectedItem);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Path for label of the item. If {@code items} is an array of objects, the
	 * {@code itemLabelPath} is used to fetch the displayed string label for
	 * each item.
	 * </p>
	 * <p>
	 * The item label is also used for matching items when processing user
	 * input, i.e., for filtering and selecting items.
	 * </p>
	 * <p>
	 * When using item templates, the property is still needed because it is
	 * used for filtering, and for displaying the selected item value in the
	 * input box.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 */
	public String getItemLabelPath() {
		return getElement().getProperty("itemLabelPath");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Path for label of the item. If {@code items} is an array of objects, the
	 * {@code itemLabelPath} is used to fetch the displayed string label for
	 * each item.
	 * </p>
	 * <p>
	 * The item label is also used for matching items when processing user
	 * input, i.e., for filtering and selecting items.
	 * </p>
	 * <p>
	 * When using item templates, the property is still needed because it is
	 * used for filtering, and for displaying the selected item value in the
	 * input box.
	 * </p>
	 * 
	 * @param itemLabelPath
	 *            the String value to set
	 */
	public void setItemLabelPath(java.lang.String itemLabelPath) {
		getElement().setProperty("itemLabelPath",
				itemLabelPath == null ? "" : itemLabelPath);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Path for the value of the item. If {@code items} is an array of objects,
	 * the {@code itemValuePath:} is used to fetch the string value for the
	 * selected item.
	 * </p>
	 * <p>
	 * The item value is used in the {@code value} property of the combo box, to
	 * provide the form value.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 */
	public String getItemValuePath() {
		return getElement().getProperty("itemValuePath");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Path for the value of the item. If {@code items} is an array of objects,
	 * the {@code itemValuePath:} is used to fetch the string value for the
	 * selected item.
	 * </p>
	 * <p>
	 * The item value is used in the {@code value} property of the combo box, to
	 * provide the form value.
	 * </p>
	 * 
	 * @param itemValuePath
	 *            the String value to set
	 */
	public void setItemValuePath(java.lang.String itemValuePath) {
		getElement().setProperty("itemValuePath",
				itemValuePath == null ? "" : itemValuePath);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Returns a reference to the native input element.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 */
	protected JsonObject protectedGetInputElement() {
		return (JsonObject) getElement().getPropertyRaw("inputElement");
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
	 * Set to true to auto-validate the input value.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 */
	public boolean isAutoValidate() {
		return getElement().getProperty("autoValidate", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set to true to auto-validate the input value.
	 * </p>
	 * 
	 * @param autoValidate
	 *            the boolean value to set
	 */
	public void setAutoValidate(boolean autoValidate) {
		getElement().setProperty("autoValidate", autoValidate);
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
	 * True when the input field has focus.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'focused-changed' event happens.
	 * </p>
	 */
	@Synchronize(property = "focused", value = "focused-changed")
	public boolean isFocused() {
		return getElement().getProperty("focused", false);
	}

	/**
	 * @return It would return a boolean
	 */
	@NotSupported
	protected void hasValidator() {
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Returns true if the {@code value} is valid, and updates {@code invalid}.
	 * If you want your element to have custom validation logic, do not override
	 * this method; override {@code _getValidity(value)} instead.
	 * </p>
	 * 
	 * @param value
	 *            Deprecated: The value to be validated. By default, it is
	 *            passed to the validator's `validate()` function, if a
	 *            validator is set. If this argument is not specified, then the
	 *            element's `value` property is used, if it exists.
	 * @return It would return a boolean
	 */
	@NotSupported
	protected void validate(elemental.json.JsonObject value) {
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Generates an anonymous {@code TemplateInstance} class (stored as
	 * {@code this.ctor}) for the provided template. This method should be
	 * called once per template to prepare an element for stamping the template,
	 * followed by {@code stamp} to create new instances of the template.
	 * </p>
	 * 
	 * @param template
	 *            Template to prepare
	 * @param mutableData
	 *            When `true`, the generated class will skip strict
	 *            dirty-checking for objects and arrays (always consider them to
	 *            be "dirty"). Defaults to false.
	 */
	protected void templatize(elemental.json.JsonObject template,
			elemental.json.JsonObject mutableData) {
		getElement().callFunction("templatize", template, mutableData);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Creates an instance of the template prepared by {@code templatize}. The
	 * object returned is an instance of the anonymous class generated by
	 * {@code templatize} whose {@code root} property is a document fragment
	 * containing newly cloned template content, and which has property
	 * accessors corresponding to properties referenced in template bindings.
	 * </p>
	 * 
	 * @param model
	 *            Object containing initial property values to populate into the
	 *            template bindings.
	 * @return It would return a interface elemental.json.JsonObject
	 */
	@NotSupported
	protected void stamp(elemental.json.JsonObject model) {
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Returns the template &quot;model&quot; ({@code TemplateInstance})
	 * associated with a given element, which serves as the binding scope for
	 * the template instance the element is contained in. A template model
	 * should be used to manipulate data associated with this template instance.
	 * </p>
	 * 
	 * @param el
	 *            Element for which to return a template model.
	 * @return It would return a interface elemental.json.JsonObject
	 */
	@NotSupported
	protected void modelForElement(elemental.json.JsonObject el) {
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
	 * Sets focus on the input field.
	 * </p>
	 */
	public void focus() {
		getElement().callFunction("focus");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Removes focus from the input field.
	 * </p>
	 */
	public void blur() {
		getElement().callFunction("blur");
	}

	@DomEvent("invalid-changed")
	public static class InvalidChangeEvent<R extends GeneratedVaadinComboBox<R>>
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

	@DomEvent("iron-form-element-register")
	public static class IronFormElementRegisterEvent<R extends GeneratedVaadinComboBox<R>>
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
	public static class IronFormElementUnregisterEvent<R extends GeneratedVaadinComboBox<R>>
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

	@DomEvent("vaadin-dropdown-closed")
	public static class VaadinDropdownClosedEvent<R extends GeneratedVaadinComboBox<R>>
			extends
				ComponentEvent<R> {
		public VaadinDropdownClosedEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addVaadinDropdownClosedListener(
			ComponentEventListener<VaadinDropdownClosedEvent<R>> listener) {
		return addListener(VaadinDropdownClosedEvent.class,
				(ComponentEventListener) listener);
	}

	@DomEvent("vaadin-dropdown-opened")
	public static class VaadinDropdownOpenedEvent<R extends GeneratedVaadinComboBox<R>>
			extends
				ComponentEvent<R> {
		public VaadinDropdownOpenedEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addVaadinDropdownOpenedListener(
			ComponentEventListener<VaadinDropdownOpenedEvent<R>> listener) {
		return addListener(VaadinDropdownOpenedEvent.class,
				(ComponentEventListener) listener);
	}

	@DomEvent("opened-changed")
	public static class OpenedChangeEvent<R extends GeneratedVaadinComboBox<R>>
			extends
				ComponentEvent<R> {
		public OpenedChangeEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addOpenedChangeListener(
			ComponentEventListener<OpenedChangeEvent<R>> listener) {
		return addListener(OpenedChangeEvent.class,
				(ComponentEventListener) listener);
	}

	@DomEvent("filter-changed")
	public static class FilterChangeEvent<R extends GeneratedVaadinComboBox<R>>
			extends
				ComponentEvent<R> {
		public FilterChangeEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addFilterChangeListener(
			ComponentEventListener<FilterChangeEvent<R>> listener) {
		return addListener(FilterChangeEvent.class,
				(ComponentEventListener) listener);
	}

	@DomEvent("selected-item-changed")
	public static class SelectedItemChangeEvent<R extends GeneratedVaadinComboBox<R>>
			extends
				ComponentEvent<R> {
		private final JsonObject detail;
		private final JsonObject detailValue;

		public SelectedItemChangeEvent(
				R source,
				boolean fromClient,
				@EventData("event.detail") elemental.json.JsonObject detail,
				@EventData("event.detail.value") elemental.json.JsonObject detailValue) {
			super(source, fromClient);
			this.detail = detail;
			this.detailValue = detailValue;
		}

		public JsonObject getDetail() {
			return detail;
		}

		public JsonObject getDetailValue() {
			return detailValue;
		}
	}

	public Registration addSelectedItemChangeListener(
			ComponentEventListener<SelectedItemChangeEvent<R>> listener) {
		return addListener(SelectedItemChangeEvent.class,
				(ComponentEventListener) listener);
	}

	@DomEvent("change")
	public static class ChangeEvent<R extends GeneratedVaadinComboBox<R>>
			extends
				ComponentEvent<R> {
		public ChangeEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addChangeListener(
			ComponentEventListener<ChangeEvent<R>> listener) {
		return addListener(ChangeEvent.class, (ComponentEventListener) listener);
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

	@DomEvent("focused-changed")
	public static class FocusedChangeEvent<R extends GeneratedVaadinComboBox<R>>
			extends
				ComponentEvent<R> {
		public FocusedChangeEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addFocusedChangeListener(
			ComponentEventListener<FocusedChangeEvent<R>> listener) {
		return addListener(FocusedChangeEvent.class,
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
	 * Adds the given components as children of this component at the slot
	 * 'clear-button'.
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
	public R addToClearButton(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			component.getElement().setAttribute("slot", "clear-button");
			getElement().appendChild(component.getElement());
		}
		return get();
	}

	/**
	 * Adds the given components as children of this component at the slot
	 * 'toggle-button'.
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
	public R addToToggleButton(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			component.getElement().setAttribute("slot", "toggle-button");
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