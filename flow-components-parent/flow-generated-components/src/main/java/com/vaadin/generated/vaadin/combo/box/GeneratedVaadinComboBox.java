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
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code <vaadin-combo-box>} is a combo box element combining a dropdown list
 * with an input field for filtering the list of items. If you want to replace
 * the default input field with a custom implementation, you should use the [
 * {@code <vaadin-combo-box-light>}](#vaadin-combo-box-light) element.
 * 
 * Items in the dropdown list must be provided as a list of {@code String}
 * values. Defining the items is done using the {@code items} property, which
 * can be assigned with data-binding, using an attribute or directly with the
 * JavaScript property.
 * 
 * {@code }`html <vaadin-combo-box label="Fruit" items="[[data]]">
 * </vaadin-combo-box> {@code }`
 * 
 * {@code }`js combobox.items = ['apple', 'orange', 'banana']; {@code }`
 * 
 * When the selected {@code value} is changed, a {@code value-changed} event is
 * triggered.
 * 
 * This element is also extended with the {@code IronFormElementBehavior} to
 * enable usage within an {@code iron-form}.
 * 
 * ### Item Template
 * 
 * {@code <vaadin-combo-box>} supports using custom item template provided in
 * the light DOM:
 * 
 * {@code }`html <vaadin-combo-box items='[{"label": "Hydrogen", "value": "H"}]'>
 * <template> [[index]]: [[item.label]] <b>[[item.value]</b> </template>
 * </vaadin-combo-box> {@code }`
 * 
 * The following properties are available for item template bindings:
 * 
 * Property name | Type | Description --------------|------|------------
 * {@code index}| Number | Index of the item in the {@code items} array
 * {@code item} | String or Object | The item reference {@code selected} |
 * Boolean | True when item is selected {@code focused} | Boolean | True when
 * item is focused
 * 
 * See the [Item Template Live Demos](demo/item-template.html) for more
 * examples.
 * 
 * ### Styling There are custom properties and mixins you can use to style the
 * component:
 * 
 * Custom property | Description | Default
 * ----------------|-------------|-------------
 * {@code --vaadin-combo-box-overlay-max-height} | Property that determines the
 * max height of overlay | {@code 65vh}
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.16-SNAPSHOT",
		"WebComponent: vaadin-combo-box#2.0.0", "Flow#0.1.16-SNAPSHOT"})
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
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Name of the validator to use.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getValidator() {
		return getElement().getProperty("validator");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Name of the validator to use.
	 * 
	 * @param validator
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setValidator(java.lang.String validator) {
		getElement().setProperty("validator",
				validator == null ? "" : validator);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the last call to {@code validate} is invalid.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'invalid-changed' event happens.
	 */
	@Synchronize(property = "invalid", value = "invalid-changed")
	public boolean isInvalid() {
		return getElement().getProperty("invalid", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the last call to {@code validate} is invalid.
	 * 
	 * @param invalid
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setInvalid(boolean invalid) {
		getElement().setProperty("invalid", invalid);
		return get();
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
	 * @return this instance, for method chaining
	 */
	public R setName(java.lang.String name) {
		getElement().setProperty("name", name == null ? "" : name);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The {@code String} value for the selected item of the combo box. Provides
	 * the value for {@code iron-form}.
	 * 
	 * When there’s no item selected, the value is an empty string.
	 * 
	 * Use {@code selectedItem} property to get the raw selected item from the
	 * {@code items} array.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'value-changed' event happens.
	 */
	@Synchronize(property = "value", value = "value-changed")
	@Override
	public String getValue() {
		return getElement().getProperty("value");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The {@code String} value for the selected item of the combo box. Provides
	 * the value for {@code iron-form}.
	 * 
	 * When there’s no item selected, the value is an empty string.
	 * 
	 * Use {@code selectedItem} property to get the raw selected item from the
	 * {@code items} array.
	 * 
	 * @param value
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	@Override
	public R setValue(java.lang.String value) {
		getElement().setProperty("value", value == null ? "" : value);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to mark the input as required.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isRequired() {
		return getElement().getProperty("required", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to mark the input as required.
	 * 
	 * @param required
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setRequired(boolean required) {
		getElement().setProperty("required", required);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the dropdown is open, false otherwise.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'opened-changed' event happens.
	 */
	@Synchronize(property = "opened", value = "opened-changed")
	public boolean isOpened() {
		return getElement().getProperty("opened", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the dropdown is open, false otherwise.
	 * 
	 * @param opened
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setOpened(boolean opened) {
		getElement().setProperty("opened", opened);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable this input.
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
	 * Set to true to disable this input.
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
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isReadonly() {
		return getElement().getProperty("readonly", false);
	}

	/**
	 * @param readonly
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setReadonly(boolean readonly) {
		getElement().setProperty("readonly", readonly);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A full set of items to filter the visible options from. The items can be
	 * of either {@code String} or {@code Object} type.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	protected JsonArray protectedGetItems() {
		return (JsonArray) getElement().getPropertyRaw("items");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A full set of items to filter the visible options from. The items can be
	 * of either {@code String} or {@code Object} type.
	 * 
	 * @param items
	 *            the JsonArray value to set
	 * @return this instance, for method chaining
	 */
	protected R setItems(elemental.json.JsonArray items) {
		getElement().setPropertyJson("items", items);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If {@code true}, the user can input a value that is not present in the
	 * items list. {@code value} property will be set to the input value in this
	 * case. Also, when {@code value} is set programmatically, the input value
	 * will be set to reflect that value.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isAllowCustomValue() {
		return getElement().getProperty("allowCustomValue", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If {@code true}, the user can input a value that is not present in the
	 * items list. {@code value} property will be set to the input value in this
	 * case. Also, when {@code value} is set programmatically, the input value
	 * will be set to reflect that value.
	 * 
	 * @param allowCustomValue
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setAllowCustomValue(boolean allowCustomValue) {
		getElement().setProperty("allowCustomValue", allowCustomValue);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A subset of items, filtered based on the user input. Filtered items can
	 * be assigned directly to omit the internal filtering functionality. The
	 * items can be of either {@code String} or {@code Object} type.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	protected JsonArray protectedGetFilteredItems() {
		return (JsonArray) getElement().getPropertyRaw("filteredItems");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A subset of items, filtered based on the user input. Filtered items can
	 * be assigned directly to omit the internal filtering functionality. The
	 * items can be of either {@code String} or {@code Object} type.
	 * 
	 * @param filteredItems
	 *            the JsonArray value to set
	 * @return this instance, for method chaining
	 */
	protected R setFilteredItems(elemental.json.JsonArray filteredItems) {
		getElement().setPropertyJson("filteredItems", filteredItems);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A read-only property indicating whether this combo box has a value
	 * selected or not. It can be used for example in styling of the component.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean hasValue() {
		return getElement().getProperty("hasValue", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When set to {@code true}, a loading spinner is displayed on top of the
	 * list of options.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isLoading() {
		return getElement().getProperty("loading", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When set to {@code true}, a loading spinner is displayed on top of the
	 * list of options.
	 * 
	 * @param loading
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setLoading(boolean loading) {
		getElement().setProperty("loading", loading);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Filtering string the user has typed into the input field.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'filter-changed' event happens.
	 */
	@Synchronize(property = "filter", value = "filter-changed")
	public String getFilter() {
		return getElement().getProperty("filter");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Filtering string the user has typed into the input field.
	 * 
	 * @param filter
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setFilter(java.lang.String filter) {
		getElement().setProperty("filter", filter == null ? "" : filter);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The selected item from the {@code items} array.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	protected JsonObject protectedGetSelectedItem() {
		return (JsonObject) getElement().getPropertyRaw("selectedItem");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The selected item from the {@code items} array.
	 * 
	 * @param selectedItem
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	protected R setSelectedItem(elemental.json.JsonObject selectedItem) {
		getElement().setPropertyJson("selectedItem", selectedItem);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Path for label of the item. If {@code items} is an array of objects, the
	 * {@code itemLabelPath} is used to fetch the displayed string label for
	 * each item.
	 * 
	 * The item label is also used for matching items when processing user
	 * input, i.e., for filtering and selecting items.
	 * 
	 * When using item templates, the property is still needed because it is
	 * used for filtering, and for displaying the selected item value in the
	 * input box.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getItemLabelPath() {
		return getElement().getProperty("itemLabelPath");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Path for label of the item. If {@code items} is an array of objects, the
	 * {@code itemLabelPath} is used to fetch the displayed string label for
	 * each item.
	 * 
	 * The item label is also used for matching items when processing user
	 * input, i.e., for filtering and selecting items.
	 * 
	 * When using item templates, the property is still needed because it is
	 * used for filtering, and for displaying the selected item value in the
	 * input box.
	 * 
	 * @param itemLabelPath
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setItemLabelPath(java.lang.String itemLabelPath) {
		getElement().setProperty("itemLabelPath",
				itemLabelPath == null ? "" : itemLabelPath);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Path for the value of the item. If {@code items} is an array of objects,
	 * the {@code itemValuePath:} is used to fetch the string value for the
	 * selected item.
	 * 
	 * The item value is used in the {@code value} property of the combo box, to
	 * provide the form value.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getItemValuePath() {
		return getElement().getProperty("itemValuePath");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Path for the value of the item. If {@code items} is an array of objects,
	 * the {@code itemValuePath:} is used to fetch the string value for the
	 * selected item.
	 * 
	 * The item value is used in the {@code value} property of the combo box, to
	 * provide the form value.
	 * 
	 * @param itemValuePath
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setItemValuePath(java.lang.String itemValuePath) {
		getElement().setProperty("itemValuePath",
				itemValuePath == null ? "" : itemValuePath);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns a reference to the native input element.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	protected JsonObject protectedGetInputElement() {
		return (JsonObject) getElement().getPropertyRaw("inputElement");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The label for this element.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getLabel() {
		return getElement().getProperty("label");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The label for this element.
	 * 
	 * @param label
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setLabel(java.lang.String label) {
		getElement().setProperty("label", label == null ? "" : label);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable the floating label.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isNoLabelFloat() {
		return getElement().getProperty("noLabelFloat", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable the floating label.
	 * 
	 * @param noLabelFloat
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setNoLabelFloat(boolean noLabelFloat) {
		getElement().setProperty("noLabelFloat", noLabelFloat);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to always float the label.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isAlwaysFloatLabel() {
		return getElement().getProperty("alwaysFloatLabel", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to always float the label.
	 * 
	 * @param alwaysFloatLabel
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setAlwaysFloatLabel(boolean alwaysFloatLabel) {
		getElement().setProperty("alwaysFloatLabel", alwaysFloatLabel);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to auto-validate the input value.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isAutoValidate() {
		return getElement().getProperty("autoValidate", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to auto-validate the input value.
	 * 
	 * @param autoValidate
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setAutoValidate(boolean autoValidate) {
		getElement().setProperty("autoValidate", autoValidate);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to prevent the user from entering invalid input.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isPreventInvalidInput() {
		return getElement().getProperty("preventInvalidInput", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to prevent the user from entering invalid input.
	 * 
	 * @param preventInvalidInput
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setPreventInvalidInput(boolean preventInvalidInput) {
		getElement().setProperty("preventInvalidInput", preventInvalidInput);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set this to specify the pattern allowed by {@code preventInvalidInput}.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getAllowedPattern() {
		return getElement().getProperty("allowedPattern");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set this to specify the pattern allowed by {@code preventInvalidInput}.
	 * 
	 * @param allowedPattern
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setAllowedPattern(java.lang.String allowedPattern) {
		getElement().setProperty("allowedPattern",
				allowedPattern == null ? "" : allowedPattern);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A pattern to validate the {@code input} with.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getPattern() {
		return getElement().getProperty("pattern");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A pattern to validate the {@code input} with.
	 * 
	 * @param pattern
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setPattern(java.lang.String pattern) {
		getElement().setProperty("pattern", pattern == null ? "" : pattern);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The error message to display when the input is invalid.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getErrorMessage() {
		return getElement().getProperty("errorMessage");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The error message to display when the input is invalid.
	 * 
	 * @param errorMessage
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setErrorMessage(java.lang.String errorMessage) {
		getElement().setProperty("errorMessage",
				errorMessage == null ? "" : errorMessage);
		return get();
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
	 * @return this instance, for method chaining
	 */
	public R setAutofocus(boolean autofocus) {
		getElement().setProperty("autofocus", autofocus);
		return get();
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
	 * @return this instance, for method chaining
	 */
	public R setInputmode(java.lang.String inputmode) {
		getElement().setProperty("inputmode",
				inputmode == null ? "" : inputmode);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A placeholder string in addition to the label. If this is set, the label
	 * will always float.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getPlaceholder() {
		return getElement().getProperty("placeholder");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A placeholder string in addition to the label. If this is set, the label
	 * will always float.
	 * 
	 * @param placeholder
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setPlaceholder(java.lang.String placeholder) {
		getElement().setProperty("placeholder",
				placeholder == null ? "" : placeholder);
		return get();
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
	 * @return this instance, for method chaining
	 */
	public R setSize(double size) {
		getElement().setProperty("size", size);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True when the input field has focus.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'focused-changed' event happens.
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
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns true if the {@code value} is valid, and updates {@code invalid}.
	 * If you want your element to have custom validation logic, do not override
	 * this method; override {@code _getValidity(value)} instead.
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
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Generates an anonymous {@code TemplateInstance} class (stored as
	 * {@code this.ctor}) for the provided template. This method should be
	 * called once per template to prepare an element for stamping the template,
	 * followed by {@code stamp} to create new instances of the template.
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
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Creates an instance of the template prepared by {@code templatize}. The
	 * object returned is an instance of the anonymous class generated by
	 * {@code templatize} whose {@code root} property is a document fragment
	 * containing newly cloned template content, and which has property
	 * accessors corresponding to properties referenced in template bindings.
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
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the template "model" ({@code TemplateInstance}) associated with a
	 * given element, which serves as the binding scope for the template
	 * instance the element is contained in. A template model should be used to
	 * manipulate data associated with this template instance.
	 * 
	 * @param el
	 *            Element for which to return a template model.
	 * @return It would return a interface elemental.json.JsonObject
	 */
	@NotSupported
	protected void modelForElement(elemental.json.JsonObject el) {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Opens the dropdown list.
	 */
	public void open() {
		getElement().callFunction("open");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Closes the dropdown list.
	 */
	public void close() {
		getElement().callFunction("close");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Reverts back to original value.
	 */
	public void cancel() {
		getElement().callFunction("cancel");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Sets focus on the input field.
	 */
	public void focus() {
		getElement().callFunction("focus");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Removes focus from the input field.
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
	 */
	public void addToPrefix(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			component.getElement().setAttribute("slot", "prefix");
			getElement().appendChild(component.getElement());
		}
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
	 */
	public void addToSuffix(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			component.getElement().setAttribute("slot", "suffix");
			getElement().appendChild(component.getElement());
		}
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
	 */
	public void addToClearButton(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			component.getElement().setAttribute("slot", "clear-button");
			getElement().appendChild(component.getElement());
		}
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
	 */
	public void addToToggleButton(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			component.getElement().setAttribute("slot", "toggle-button");
			getElement().appendChild(component.getElement());
		}
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