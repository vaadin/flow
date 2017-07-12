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
package com.vaadin.components.vaadin.combo.box;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.components.vaadin.combo.box.VaadinComboBoxLight;
import com.vaadin.annotations.Synchronize;
import elemental.json.JsonArray;
import com.vaadin.components.NotSupported;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;
import com.vaadin.annotations.EventData;
import com.vaadin.ui.HasComponents;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code <vaadin-combo-box-light>} is a customizable version of the
 * {@code <vaadin-combo-box>} providing only the dropdown functionality and
 * leaving the input field definition to the user.
 * 
 * This element is using the same [{@code ComboBoxBehavior}
 * ](#vaadin.elements.combobox.ComboBoxBehavior) as [{@code <vaadin-combo-box>}
 * ](#vaadin-combo-box), so the API remains the same.
 * 
 * To create a custom input field, you need to add a child element which has a
 * two-way data-bindable property representing the input value. The property
 * name is expected to be {@code bindValue} by default. See the example below
 * for a simplest possible example using an {@code <input>} element extended
 * with {@code iron-input}.
 * 
 * {@code }`html <vaadin-combo-box-light> <iron-input> <input> </iron-input>
 * </vaadin-combo-box-light> {@code }`
 * 
 * If you are using other custom input fields like {@code <paper-input>}, you
 * need to define the name of value property with {@code attrForValue} property.
 * See the example below on how to create a custom input field based on a
 * {@code <paper-input>} decorated with a custom {@code <iron-icon>} and two
 * {@code <paper-button>}s to act as the clear and toggle controls.
 * 
 * {@code }`html <vaadin-combo-box-light attr-for-value="value"> <paper-input
 * label="Elements" class="input"> <iron-icon icon="toll" prefix></iron-icon>
 * <paper-button suffix class="clear-button">Clear</paper-button> <paper-button
 * suffix class="toggle-button">Toggle</paper-button> </paper-input>
 * </vaadin-combo-box-light> {@code }`
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.13-SNAPSHOT",
		"WebComponent: vaadin-combo-box-light#null", "Flow#0.1.13-SNAPSHOT"})
@Tag("vaadin-combo-box-light")
@HtmlImport("frontend://bower_components/vaadin-combo-box/vaadin-combo-box-light.html")
public class VaadinComboBoxLight extends Component
		implements
			HasStyle,
			HasComponents {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The EventTarget that will be firing relevant KeyboardEvents. Set it to
	 * {@code null} to disable the listeners.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getKeyEventTarget() {
		return (JsonObject) getElement().getPropertyRaw("keyEventTarget");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The EventTarget that will be firing relevant KeyboardEvents. Set it to
	 * {@code null} to disable the listeners.
	 * 
	 * @param keyEventTarget
	 *            The JsonObject value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setKeyEventTarget(
			elemental.json.JsonObject keyEventTarget) {
		getElement().setPropertyJson("keyEventTarget", keyEventTarget);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, this property will cause the implementing element to
	 * automatically stop propagation on any handled KeyboardEvents.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isStopKeyboardEventPropagation() {
		return getElement().getProperty("stopKeyboardEventPropagation", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, this property will cause the implementing element to
	 * automatically stop propagation on any handled KeyboardEvents.
	 * 
	 * @param stopKeyboardEventPropagation
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setStopKeyboardEventPropagation(
			boolean stopKeyboardEventPropagation) {
		getElement().setProperty("stopKeyboardEventPropagation",
				stopKeyboardEventPropagation);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * To be used to express what combination of keys will trigger the relative
	 * callback. e.g. {@code keyBindings: 'esc': '_onEscPressed'}}
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getKeyBindings() {
		return (JsonObject) getElement().getPropertyRaw("keyBindings");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * To be used to express what combination of keys will trigger the relative
	 * callback. e.g. {@code keyBindings: 'esc': '_onEscPressed'}}
	 * 
	 * @param keyBindings
	 *            The JsonObject value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setKeyBindings(
			elemental.json.JsonObject keyBindings) {
		getElement().setPropertyJson("keyBindings", keyBindings);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of this element.
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
	 * The name of this element.
	 * 
	 * @param name
	 *            The String value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setName(java.lang.String name) {
		getElement().setProperty("name", name == null ? "" : name);
		return getSelf();
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
	 *            The String value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setValue(java.lang.String value) {
		getElement().setProperty("value", value == null ? "" : value);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to mark the input as required. If used in a form, a custom
	 * element that uses this behavior should also use
	 * Polymer.IronValidatableBehavior and define a custom validation method.
	 * Otherwise, a {@code required} element will always be considered valid.
	 * It's also strongly recommended to provide a visual style for the element
	 * when its value is invalid.
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
	 * Set to true to mark the input as required. If used in a form, a custom
	 * element that uses this behavior should also use
	 * Polymer.IronValidatableBehavior and define a custom validation method.
	 * Otherwise, a {@code required} element will always be considered valid.
	 * It's also strongly recommended to provide a visual style for the element
	 * when its value is invalid.
	 * 
	 * @param required
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setRequired(boolean required) {
		getElement().setProperty("required", required);
		return getSelf();
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
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setOpened(boolean opened) {
		getElement().setProperty("opened", opened);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable this element.
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
	 * Set to true to disable this element.
	 * 
	 * @param disabled
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setDisabled(boolean disabled) {
		getElement().setProperty("disabled", disabled);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When present, it specifies that the element field is read-only.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isReadonly() {
		return getElement().getProperty("readonly", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When present, it specifies that the element field is read-only.
	 * 
	 * @param readonly
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setReadonly(boolean readonly) {
		getElement().setProperty("readonly", readonly);
		return getSelf();
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
	public JsonArray getItems() {
		return (JsonArray) getElement().getPropertyRaw("items");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A full set of items to filter the visible options from. The items can be
	 * of either {@code String} or {@code Object} type.
	 * 
	 * @param items
	 *            The JsonArray value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setItems(
			elemental.json.JsonArray items) {
		getElement().setPropertyJson("items", items);
		return getSelf();
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
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setAllowCustomValue(
			boolean allowCustomValue) {
		getElement().setProperty("allowCustomValue", allowCustomValue);
		return getSelf();
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
	public JsonArray getFilteredItems() {
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
	 *            The JsonArray value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setFilteredItems(
			elemental.json.JsonArray filteredItems) {
		getElement().setPropertyJson("filteredItems", filteredItems);
		return getSelf();
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
	public boolean isHasValue() {
		return getElement().getProperty("hasValue", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A read-only property indicating whether this combo box has a value
	 * selected or not. It can be used for example in styling of the component.
	 * 
	 * @param hasValue
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setHasValue(boolean hasValue) {
		getElement().setProperty("hasValue", hasValue);
		return getSelf();
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
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setLoading(boolean loading) {
		getElement().setProperty("loading", loading);
		return getSelf();
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
	 *            The String value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setFilter(java.lang.String filter) {
		getElement().setProperty("filter", filter == null ? "" : filter);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The selected item from the {@code items} array.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getSelectedItem() {
		return (JsonObject) getElement().getPropertyRaw("selectedItem");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The selected item from the {@code items} array.
	 * 
	 * @param selectedItem
	 *            The JsonObject value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setSelectedItem(
			elemental.json.JsonObject selectedItem) {
		getElement().setPropertyJson("selectedItem", selectedItem);
		return getSelf();
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
	 *            The String value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setItemLabelPath(
			java.lang.String itemLabelPath) {
		getElement().setProperty("itemLabelPath",
				itemLabelPath == null ? "" : itemLabelPath);
		return getSelf();
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
	 *            The String value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setItemValuePath(
			java.lang.String itemValuePath) {
		getElement().setProperty("itemValuePath",
				itemValuePath == null ? "" : itemValuePath);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns a reference to the native input element.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getInputElement() {
		return (JsonObject) getElement().getPropertyRaw("inputElement");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns a reference to the native input element.
	 * 
	 * @param inputElement
	 *            The JsonObject value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setInputElement(
			elemental.json.JsonObject inputElement) {
		getElement().setPropertyJson("inputElement", inputElement);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Name of the two-way data-bindable property representing the value of the
	 * custom input field.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getAttrForValue() {
		return getElement().getProperty("attrForValue");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Name of the two-way data-bindable property representing the value of the
	 * custom input field.
	 * 
	 * @param attrForValue
	 *            The String value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setAttrForValue(
			java.lang.String attrForValue) {
		getElement().setProperty("attrForValue",
				attrForValue == null ? "" : attrForValue);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Number of pixels used as the vertical offset in positioning of the
	 * dropdown.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getOverlayVerticalOffset() {
		return getElement().getProperty("overlayVerticalOffset", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Number of pixels used as the vertical offset in positioning of the
	 * dropdown.
	 * 
	 * @param overlayVerticalOffset
	 *            The double value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinComboBoxLight> R setOverlayVerticalOffset(
			double overlayVerticalOffset) {
		getElement()
				.setProperty("overlayVerticalOffset", overlayVerticalOffset);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Can be used to imperatively add a key binding to the implementing
	 * element. This is the imperative equivalent of declaring a keybinding in
	 * the {@code keyBindings} prototype property.
	 * 
	 * @param eventString
	 *            Missing documentation!
	 * @param handlerName
	 *            Missing documentation!
	 */
	public void addOwnKeyBinding(java.lang.String eventString,
			java.lang.String handlerName) {
		getElement().callFunction("addOwnKeyBinding", eventString, handlerName);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When called, will remove all imperatively-added key bindings.
	 */
	public void removeOwnKeyBindings() {
		getElement().callFunction("removeOwnKeyBindings");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns true if a keyboard event matches {@code eventString}.
	 * 
	 * @param event
	 *            Missing documentation!
	 * @param eventString
	 *            Missing documentation!
	 * @return It would return a boolean
	 */
	@NotSupported
	protected void keyboardEventMatchesKeys(elemental.json.JsonObject event,
			java.lang.String eventString) {
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
	public void templatize(elemental.json.JsonObject template,
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

	@DomEvent("iron-form-element-register")
	public static class IronFormElementRegisterEvent
			extends
				ComponentEvent<VaadinComboBoxLight> {
		public IronFormElementRegisterEvent(VaadinComboBoxLight source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronFormElementRegisterListener(
			ComponentEventListener<IronFormElementRegisterEvent> listener) {
		return addListener(IronFormElementRegisterEvent.class, listener);
	}

	@DomEvent("iron-form-element-unregister")
	public static class IronFormElementUnregisterEvent
			extends
				ComponentEvent<VaadinComboBoxLight> {
		public IronFormElementUnregisterEvent(VaadinComboBoxLight source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronFormElementUnregisterListener(
			ComponentEventListener<IronFormElementUnregisterEvent> listener) {
		return addListener(IronFormElementUnregisterEvent.class, listener);
	}

	@DomEvent("value-changed")
	public static class ValueChangedEvent
			extends
				ComponentEvent<VaadinComboBoxLight> {
		private final JsonObject detail;
		private final String detailValue;

		public ValueChangedEvent(VaadinComboBoxLight source,
				boolean fromClient,
				@EventData("event.detail") elemental.json.JsonObject detail,
				@EventData("event.detail.value") java.lang.String detailValue) {
			super(source, fromClient);
			this.detail = detail;
			this.detailValue = detailValue;
		}

		public JsonObject getDetail() {
			return detail;
		}

		public String getDetailValue() {
			return detailValue;
		}
	}

	public Registration addValueChangedListener(
			ComponentEventListener<ValueChangedEvent> listener) {
		return addListener(ValueChangedEvent.class, listener);
	}

	@DomEvent("vaadin-dropdown-closed")
	public static class VaadinDropdownClosedEvent
			extends
				ComponentEvent<VaadinComboBoxLight> {
		public VaadinDropdownClosedEvent(VaadinComboBoxLight source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addVaadinDropdownClosedListener(
			ComponentEventListener<VaadinDropdownClosedEvent> listener) {
		return addListener(VaadinDropdownClosedEvent.class, listener);
	}

	@DomEvent("vaadin-dropdown-opened")
	public static class VaadinDropdownOpenedEvent
			extends
				ComponentEvent<VaadinComboBoxLight> {
		public VaadinDropdownOpenedEvent(VaadinComboBoxLight source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addVaadinDropdownOpenedListener(
			ComponentEventListener<VaadinDropdownOpenedEvent> listener) {
		return addListener(VaadinDropdownOpenedEvent.class, listener);
	}

	@DomEvent("opened-changed")
	public static class OpenedChangedEvent
			extends
				ComponentEvent<VaadinComboBoxLight> {
		public OpenedChangedEvent(VaadinComboBoxLight source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addOpenedChangedListener(
			ComponentEventListener<OpenedChangedEvent> listener) {
		return addListener(OpenedChangedEvent.class, listener);
	}

	@DomEvent("filter-changed")
	public static class FilterChangedEvent
			extends
				ComponentEvent<VaadinComboBoxLight> {
		public FilterChangedEvent(VaadinComboBoxLight source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addFilterChangedListener(
			ComponentEventListener<FilterChangedEvent> listener) {
		return addListener(FilterChangedEvent.class, listener);
	}

	@DomEvent("selected-item-changed")
	public static class SelectedItemChangedEvent
			extends
				ComponentEvent<VaadinComboBoxLight> {
		private final JsonObject detail;
		private final JsonObject detailValue;

		public SelectedItemChangedEvent(
				VaadinComboBoxLight source,
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

	public Registration addSelectedItemChangedListener(
			ComponentEventListener<SelectedItemChangedEvent> listener) {
		return addListener(SelectedItemChangedEvent.class, listener);
	}

	@DomEvent("change")
	public static class ChangeEvent extends ComponentEvent<VaadinComboBoxLight> {
		public ChangeEvent(VaadinComboBoxLight source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addChangeListener(
			ComponentEventListener<ChangeEvent> listener) {
		return addListener(ChangeEvent.class, listener);
	}

	@DomEvent("custom-value-set")
	public static class CustomValueSetEvent
			extends
				ComponentEvent<VaadinComboBoxLight> {
		private final String detail;

		public CustomValueSetEvent(VaadinComboBoxLight source,
				boolean fromClient,
				@EventData("event.detail") java.lang.String detail) {
			super(source, fromClient);
			this.detail = detail;
		}

		public String getDetail() {
			return detail;
		}
	}

	public Registration addCustomValueSetListener(
			ComponentEventListener<CustomValueSetEvent> listener) {
		return addListener(CustomValueSetEvent.class, listener);
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected <R extends VaadinComboBoxLight> R getSelf() {
		return (R) this;
	}
}