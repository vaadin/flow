/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.component.combobox;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import elemental.json.JsonObject;
import com.vaadin.flow.component.Synchronize;
import elemental.json.JsonArray;
import com.vaadin.flow.component.NotSupported;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.component.AbstractSinglePropertyField;

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
 * &lt;vaadin-combo-box-light&gt; &lt;vaadin-text-field&gt;
 * &lt;/vaadin-text-field&gt; &lt;/vaadin-combo-box-light&gt;
 * </p>
 * <p>
 * If you are using other custom input fields like {@code <iron-input>}, you
 * need to define the name of the bindable property with the
 * {@code attrForValue} attribute.
 * </p>
 * <p>
 * &lt;vaadin-combo-box-light attr-for-value=&quot;bind-value&quot;&gt;
 * &lt;iron-input&gt; &lt;input&gt; &lt;/iron-input&gt;
 * &lt;/vaadin-combo-box-light&gt;
 * </p>
 * <p>
 * In the next example you can see how to create a custom input field based on a
 * {@code <paper-input>} decorated with a custom {@code <iron-icon>} and two
 * {@code <paper-button>}s to act as the clear and toggle controls.
 * </p>
 * <p>
 * &lt;vaadin-combo-box-light&gt; &lt;paper-input label=&quot;Elements&quot;
 * class=&quot;input&quot;&gt; &lt;iron-icon icon=&quot;toll&quot;
 * slot=&quot;prefix&quot;&gt;&lt;/iron-icon&gt; &lt;paper-button
 * slot=&quot;suffix&quot;
 * class=&quot;clear-button&quot;&gt;Clear&lt;/paper-button&gt; &lt;paper-button
 * slot=&quot;suffix&quot;
 * class=&quot;toggle-button&quot;&gt;Toggle&lt;/paper-button&gt;
 * &lt;/paper-input&gt; &lt;/vaadin-combo-box-light&gt;
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.ComboBoxLightElement#4.2.0",
        "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-combo-box-light")
@HtmlImport("frontend://bower_components/vaadin-combo-box/src/vaadin-combo-box-light.html")
public abstract class GeneratedVaadinComboBoxLight<R extends GeneratedVaadinComboBoxLight<R, T>, T>
        extends AbstractSinglePropertyField<R, T> implements HasStyle {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Number of items fetched at a time from the dataprovider.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code pageSize} property from the webcomponent
     */
    protected double getPageSizeDouble() {
        return getElement().getProperty("pageSize", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Number of items fetched at a time from the dataprovider.
     * </p>
     * 
     * @param pageSize
     *            the double value to set
     */
    protected void setPageSize(double pageSize) {
        getElement().setProperty("pageSize", pageSize);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Total number of items.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code size} property from the webcomponent
     */
    protected double getSizeDouble() {
        return getElement().getProperty("size", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Total number of items.
     * </p>
     * 
     * @param size
     *            the double value to set
     */
    protected void setSize(double size) {
        getElement().setProperty("size", size);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Function that provides items lazily. Receives arguments {@code params},
     * {@code callback}
     * </p>
     * <p>
     * {@code params.page} Requested page index
     * </p>
     * <p>
     * {@code params.pageSize} Current page size
     * </p>
     * <p>
     * {@code params.filter} Currently applied filter
     * </p>
     * <p>
     * {@code callback(items, size)} Callback function with arguments:
     * </p>
     * <ul>
     * <li>{@code items} Current page of items</li>
     * <li>{@code size} Total number of items.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.</li>
     * </ul>
     * 
     * @return the {@code dataProvider} property from the webcomponent
     */
    protected JsonObject getDataProviderJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("dataProvider");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Function that provides items lazily. Receives arguments {@code params},
     * {@code callback}
     * </p>
     * <p>
     * {@code params.page} Requested page index
     * </p>
     * <p>
     * {@code params.pageSize} Current page size
     * </p>
     * <p>
     * {@code params.filter} Currently applied filter
     * </p>
     * <p>
     * {@code callback(items, size)} Callback function with arguments:
     * </p>
     * <ul>
     * <li>{@code items} Current page of items</li>
     * <li>{@code size} Total number of items.</li>
     * </ul>
     * 
     * @param dataProvider
     *            the JsonObject value to set
     */
    protected void setDataProvider(JsonObject dataProvider) {
        getElement().setPropertyJson("dataProvider", dataProvider);
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
     * 
     * @return the {@code opened} property from the webcomponent
     */
    @Synchronize(property = "opened", value = "opened-changed")
    protected boolean isOpenedBoolean() {
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
    protected void setOpened(boolean opened) {
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
    protected boolean isDisabledBoolean() {
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
    protected void setDisabled(boolean disabled) {
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
    protected boolean isReadonlyBoolean() {
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
    protected void setReadonly(boolean readonly) {
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
    protected JsonArray getItemsJsonArray() {
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
    protected void setItems(JsonArray items) {
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
    protected boolean isAllowCustomValueBoolean() {
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
    protected void setAllowCustomValue(boolean allowCustomValue) {
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
    protected JsonArray getFilteredItemsJsonArray() {
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
    protected void setFilteredItems(JsonArray filteredItems) {
        getElement().setPropertyJson("filteredItems", filteredItems);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When set to {@code true}, &quot;loading&quot; attribute is added to host
     * and the overlay element.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code loading} property from the webcomponent
     */
    protected boolean isLoadingBoolean() {
        return getElement().getProperty("loading", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When set to {@code true}, &quot;loading&quot; attribute is added to host
     * and the overlay element.
     * </p>
     * 
     * @param loading
     *            the boolean value to set
     */
    protected void setLoading(boolean loading) {
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
     * 
     * @return the {@code filter} property from the webcomponent
     */
    @Synchronize(property = "filter", value = "filter-changed")
    protected String getFilterString() {
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
    protected void setFilter(String filter) {
        getElement().setProperty("filter", filter == null ? "" : filter);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The selected item from the {@code items} array.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'selected-item-changed' event happens.
     * </p>
     * 
     * @return the {@code selectedItem} property from the webcomponent
     */
    @Synchronize(property = "selectedItem", value = "selected-item-changed")
    protected JsonObject getSelectedItemJsonObject() {
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
    protected void setSelectedItem(JsonObject selectedItem) {
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
    protected String getItemLabelPathString() {
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
    protected void setItemLabelPath(String itemLabelPath) {
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
    protected String getItemValuePathString() {
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
    protected void setItemValuePath(String itemValuePath) {
        getElement().setProperty("itemValuePath",
                itemValuePath == null ? "" : itemValuePath);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Path for the id of the item. If {@code items} is an array of objects, the
     * {@code itemIdPath} is used to compare and identify the same item in
     * {@code selectedItem} and {@code filteredItems} (items given by the
     * {@code dataProvider} callback).
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code itemIdPath} property from the webcomponent
     */
    protected String getItemIdPathString() {
        return getElement().getProperty("itemIdPath");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Path for the id of the item. If {@code items} is an array of objects, the
     * {@code itemIdPath} is used to compare and identify the same item in
     * {@code selectedItem} and {@code filteredItems} (items given by the
     * {@code dataProvider} callback).
     * </p>
     * 
     * @param itemIdPath
     *            the String value to set
     */
    protected void setItemIdPath(String itemIdPath) {
        getElement().setProperty("itemIdPath",
                itemIdPath == null ? "" : itemIdPath);
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
    protected String getNameString() {
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
    protected void setName(String name) {
        getElement().setProperty("name", name == null ? "" : name);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true if the value is invalid.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'invalid-changed' event happens.
     * </p>
     * 
     * @return the {@code invalid} property from the webcomponent
     */
    @Synchronize(property = "invalid", value = "invalid-changed")
    protected boolean isInvalidBoolean() {
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
    protected void setInvalid(boolean invalid) {
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
    protected String getAttrForValueString() {
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
    protected void setAttrForValue(String attrForValue) {
        getElement().setProperty("attrForValue",
                attrForValue == null ? "" : attrForValue);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code inputElement} property from the webcomponent
     */
    protected JsonObject getInputElementJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("inputElement");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Clears the cached pages and reloads data from dataprovider when needed.
     * </p>
     */
    protected void clearCache() {
        getElement().callFunction("clearCache");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Manually invoke existing renderer.
     * </p>
     */
    protected void render() {
        getElement().callFunction("render");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Opens the dropdown list.
     * </p>
     */
    protected void open() {
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
    protected void close() {
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
    protected void cancel() {
        getElement().callFunction("cancel");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns true if {@code value} is valid, and sets the {@code invalid} flag
     * appropriately.
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
     * <p>
     * You can override the {@code checkValidity} method for custom validations.
     * </p>
     */
    protected void checkValidity() {
        getElement().callFunction("checkValidity");
    }

    @DomEvent("change")
    public static class ChangeEvent<R extends GeneratedVaadinComboBoxLight<R, ?>>
            extends ComponentEvent<R> {
        public ChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code change} events fired by the webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addChangeListener(
            ComponentEventListener<ChangeEvent<R>> listener) {
        return addListener(ChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("custom-value-set")
    public static class CustomValueSetEvent<R extends GeneratedVaadinComboBoxLight<R, ?>>
            extends ComponentEvent<R> {
        private final String detail;

        public CustomValueSetEvent(R source, boolean fromClient,
                @EventData("event.detail") String detail) {
            super(source, fromClient);
            this.detail = detail;
        }

        public String getDetail() {
            return detail;
        }
    }

    /**
     * Adds a listener for {@code custom-value-set} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addCustomValueSetListener(
            ComponentEventListener<CustomValueSetEvent<R>> listener) {
        return addListener(CustomValueSetEvent.class,
                (ComponentEventListener) listener);
    }

    public static class SelectedItemChangeEvent<R extends GeneratedVaadinComboBoxLight<R, ?>>
            extends ComponentEvent<R> {
        private final JsonObject selectedItem;

        public SelectedItemChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.selectedItem = source.getSelectedItemJsonObject();
        }

        public JsonObject getSelectedItem() {
            return selectedItem;
        }
    }

    /**
     * Adds a listener for {@code selected-item-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addSelectedItemChangeListener(
            ComponentEventListener<SelectedItemChangeEvent<R>> listener) {
        return getElement().addPropertyChangeListener("selectedItem",
                event -> listener.onComponentEvent(
                        new SelectedItemChangeEvent<R>((R) this,
                                event.isUserOriginated())));
    }

    public static class OpenedChangeEvent<R extends GeneratedVaadinComboBoxLight<R, ?>>
            extends ComponentEvent<R> {
        private final boolean opened;

        public OpenedChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.opened = source.isOpenedBoolean();
        }

        public boolean isOpened() {
            return opened;
        }
    }

    /**
     * Adds a listener for {@code opened-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addOpenedChangeListener(
            ComponentEventListener<OpenedChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("opened",
                        event -> listener.onComponentEvent(
                                new OpenedChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }

    public static class FilterChangeEvent<R extends GeneratedVaadinComboBoxLight<R, ?>>
            extends ComponentEvent<R> {
        private final String filter;

        public FilterChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.filter = source.getFilterString();
        }

        public String getFilter() {
            return filter;
        }
    }

    /**
     * Adds a listener for {@code filter-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addFilterChangeListener(
            ComponentEventListener<FilterChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("filter",
                        event -> listener.onComponentEvent(
                                new FilterChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }

    public static class InvalidChangeEvent<R extends GeneratedVaadinComboBoxLight<R, ?>>
            extends ComponentEvent<R> {
        private final boolean invalid;

        public InvalidChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.invalid = source.isInvalidBoolean();
        }

        public boolean isInvalid() {
            return invalid;
        }
    }

    /**
     * Adds a listener for {@code invalid-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addInvalidChangeListener(
            ComponentEventListener<InvalidChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("invalid",
                        event -> listener.onComponentEvent(
                                new InvalidChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }

    /**
     * Constructs a new GeneratedVaadinComboBoxLight component with the given
     * arguments.
     * 
     * @param initialValue
     *            the initial value to set to the value
     * @param defaultValue
     *            the default value to use if the value isn't defined
     * @param elementPropertyType
     *            the type of the element property
     * @param presentationToModel
     *            a function that converts a string value to a model value
     * @param modelToPresentation
     *            a function that converts a model value to a string value
     * @param <P>
     *            the property type
     */
    public <P> GeneratedVaadinComboBoxLight(T initialValue, T defaultValue,
            Class<P> elementPropertyType,
            SerializableFunction<P, T> presentationToModel,
            SerializableFunction<T, P> modelToPresentation) {
        super("value", defaultValue, elementPropertyType, presentationToModel,
                modelToPresentation);
        if (initialValue != null) {
            setModelValue(initialValue, false);
            setPresentationValue(initialValue);
        }
    }

    /**
     * Constructs a new GeneratedVaadinComboBoxLight component with the given
     * arguments.
     * 
     * @param initialValue
     *            the initial value to set to the value
     * @param defaultValue
     *            the default value to use if the value isn't defined
     * @param acceptNullValues
     *            whether <code>null</code> is accepted as a model value
     */
    public GeneratedVaadinComboBoxLight(T initialValue, T defaultValue,
            boolean acceptNullValues) {
        super("value", defaultValue, acceptNullValues);
        if (initialValue != null) {
            setModelValue(initialValue, false);
            setPresentationValue(initialValue);
        }
    }

    /**
     * Constructs a new GeneratedVaadinComboBoxLight component with the given
     * arguments.
     * 
     * @param initialValue
     *            the initial value to set to the value
     * @param defaultValue
     *            the default value to use if the value isn't defined
     * @param elementPropertyType
     *            the type of the element property
     * @param presentationToModel
     *            a function that accepts this component and a property value
     *            and returns a model value
     * @param modelToPresentation
     *            a function that accepts this component and a model value and
     *            returns a property value
     * @param <P>
     *            the property type
     */
    public <P> GeneratedVaadinComboBoxLight(T initialValue, T defaultValue,
            Class<P> elementPropertyType,
            SerializableBiFunction<R, P, T> presentationToModel,
            SerializableBiFunction<R, T, P> modelToPresentation) {
        super("value", defaultValue, elementPropertyType, presentationToModel,
                modelToPresentation);
        if (initialValue != null) {
            setModelValue(initialValue, false);
            setPresentationValue(initialValue);
        }
    }

    /**
     * Default constructor.
     */
    public GeneratedVaadinComboBoxLight() {
        this(null, null, null, (SerializableFunction) null,
                (SerializableFunction) null);
    }
}