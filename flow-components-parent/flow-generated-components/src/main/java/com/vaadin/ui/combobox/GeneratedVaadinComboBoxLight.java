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
import com.vaadin.ui.event.Synchronize;
import com.vaadin.ui.common.HasValue;
import java.util.Objects;
import elemental.json.JsonObject;
import com.vaadin.ui.common.NotSupported;
import com.vaadin.ui.event.DomEvent;
import com.vaadin.ui.event.ComponentEvent;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.shared.Registration;
import com.vaadin.ui.event.EventData;
import com.vaadin.ui.common.HasComponents;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-combo-box-light>} is a customizable version of the
 * {@code <vaadin-combo-box>} providing only the dropdown functionality and
 * leaving the input field definition to the user.
 * </p>
 * <p>
 * The element has the same API as {@code <vaadin-combo-box>}.
 * </p>
 * <p>
 * To create a custom input field, you need to add a child element which has a
 * two-way data-bindable property representing the input value. The property
 * name is expected to be {@code value} by default. See the example below for a
 * simplest possible example using a {@code <vaadin-text-field>} element.
 * </p>
 * <p>
 * {@code }
 * <code>html &lt;vaadin-combo-box-light&gt; &lt;vaadin-text-field&gt; &lt;/vaadin-text-field&gt; &lt;/vaadin-combo-box-light&gt; {@code }</code>
 * </p>
 * <p>
 * If you are using other custom input fields like {@code <iron-input>}, you
 * need to define the name of the bindable property with the
 * {@code attrForValue} attribute.
 * </p>
 * <p>
 * {@code }
 * <code>html &lt;vaadin-combo-box-light attr-for-value=&quot;bind-value&quot;&gt; &lt;iron-input&gt; &lt;input&gt; &lt;/iron-input&gt; &lt;/vaadin-combo-box-light&gt; {@code }</code>
 * </p>
 * <p>
 * In the next example you can see how to create a custom input field based on a
 * {@code <paper-input>} decorated with a custom {@code <iron-icon>} and two
 * {@code <paper-button>}s to act as the clear and toggle controls.
 * </p>
 * <p>
 * {@code }
 * <code>html &lt;vaadin-combo-box-light&gt; &lt;paper-input label=&quot;Elements&quot; class=&quot;input&quot;&gt; &lt;iron-icon icon=&quot;toll&quot; prefix&gt;&lt;/iron-icon&gt; &lt;paper-button slot=&quot;suffix&quot; class=&quot;clear-button&quot;&gt;Clear&lt;/paper-button&gt; &lt;paper-button slot=&quot;suffix&quot; class=&quot;toggle-button&quot;&gt;Toggle&lt;/paper-button&gt; &lt;/paper-input&gt; &lt;/vaadin-combo-box-light&gt; {@code }</code>
 * </p>
 */
@Generated({"Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
		"WebComponent: Vaadin.ComboBoxLightElement#3.0.0", "Flow#1.0-SNAPSHOT"})
@Tag("vaadin-combo-box-light")
@HtmlImport("frontend://bower_components/vaadin-combo-box/vaadin-combo-box-light.html")
public class GeneratedVaadinComboBoxLight<R extends GeneratedVaadinComboBoxLight<R>>
		extends
			Component
		implements
			ComponentSupplier<R>,
			HasStyle,
			HasValue<R, String>,
			HasComponents {

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
	 * Set to true to disable this element.
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
	 * Set to true to disable this element.
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
	 * When present, it specifies that the element field is read-only.
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
	 * When present, it specifies that the element field is read-only.
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
		if (!Objects.equals(value, getValue())) {
			getElement().setProperty("value", value == null ? "" : value);
		}
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
	 * The selected item from the {@code items} array.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code selectedItem} property from the webcomponent
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
	 * 
	 * @return the {@code itemLabelPath} property from the webcomponent
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
	 * 
	 * @return the {@code itemValuePath} property from the webcomponent
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
	 * Name of the two-way data-bindable property representing the value of the
	 * custom input field.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code attrForValue} property from the webcomponent
	 */
	public String getAttrForValue() {
		return getElement().getProperty("attrForValue");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Name of the two-way data-bindable property representing the value of the
	 * custom input field.
	 * </p>
	 * 
	 * @param attrForValue
	 *            the String value to set
	 */
	public void setAttrForValue(java.lang.String attrForValue) {
		getElement().setProperty("attrForValue",
				attrForValue == null ? "" : attrForValue);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Number of pixels used as the vertical offset in positioning of the
	 * dropdown.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code overlayVerticalOffset} property from the webcomponent
	 */
	public double getOverlayVerticalOffset() {
		return getElement().getProperty("overlayVerticalOffset", 0.0);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Number of pixels used as the vertical offset in positioning of the
	 * dropdown.
	 * </p>
	 * 
	 * @param overlayVerticalOffset
	 *            the double value to set
	 */
	public void setOverlayVerticalOffset(double overlayVerticalOffset) {
		getElement()
				.setProperty("overlayVerticalOffset", overlayVerticalOffset);
	}

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * 
	 * @return the {@code inputElement} property from the webcomponent
	 */
	protected JsonObject protectedGetInputElement() {
		return (JsonObject) getElement().getPropertyRaw("inputElement");
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
	protected void validate(elemental.json.JsonObject value) {
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

	@DomEvent("change")
	public static class ChangeEvent<R extends GeneratedVaadinComboBoxLight<R>>
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
	public static class CustomValueSetEvent<R extends GeneratedVaadinComboBoxLight<R>>
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

	@DomEvent("selected-item-changed")
	public static class SelectedItemChangeEvent<R extends GeneratedVaadinComboBoxLight<R>>
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

	/**
	 * Adds the given components as children of this component.
	 * 
	 * @param components
	 *            the components to add
	 * @see HasComponents#add(Component...)
	 */
	public GeneratedVaadinComboBoxLight(com.vaadin.ui.Component... components) {
		add(components);
	}

	/**
	 * Default constructor.
	 */
	public GeneratedVaadinComboBoxLight() {
	}
}