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
package com.vaadin.components.iron.selector;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import elemental.json.JsonArray;
import com.vaadin.components.NotSupported;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code iron-selector} is an element which can be used to manage a list of
 * elements that can be selected. Tapping on the item will make the item
 * selected. The {@code selected} indicates which item is being selected. The
 * default is to use the index of the item.
 * 
 * Example:
 * 
 * <iron-selector selected="0"> <div>Item 1</div> <div>Item 2</div> <div>Item
 * 3</div> </iron-selector>
 * 
 * If you want to use the attribute value of an element for {@code selected}
 * instead of the index, set {@code attrForSelected} to the name of the
 * attribute. For example, if you want to select item by {@code name}, set
 * {@code attrForSelected} to {@code name}.
 * 
 * Example:
 * 
 * <iron-selector attr-for-selected="name" selected="foo"> <div
 * name="foo">Foo</div> <div name="bar">Bar</div> <div name="zot">Zot</div>
 * </iron-selector>
 * 
 * You can specify a default fallback with {@code fallbackSelection} in case the
 * {@code selected} attribute does not match the {@code attrForSelected}
 * attribute of any elements.
 * 
 * Example:
 * 
 * <iron-selector attr-for-selected="name" selected="non-existing"
 * fallback-selection="default"> <div name="foo">Foo</div> <div
 * name="bar">Bar</div> <div name="default">Default</div> </iron-selector>
 * 
 * Note: When the selector is multi, the selection will set to
 * {@code fallbackSelection} iff the number of matching elements is zero.
 * 
 * {@code iron-selector} is not styled. Use the {@code iron-selected} CSS class
 * to style the selected element.
 * 
 * Example:
 * 
 * <style> .iron-selected { background: #eee; } </style>
 * 
 * ...
 * 
 * <iron-selector selected="0"> <div>Item 1</div> <div>Item 2</div> <div>Item
 * 3</div> </iron-selector>
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.12-SNAPSHOT",
		"WebComponent: iron-selector#2.0.0", "Flow#0.1.12-SNAPSHOT"})
@Tag("iron-selector")
@HtmlImport("frontend://bower_components/iron-selector/iron-selector.html")
public class IronSelector<R extends IronSelector<R>> extends Component
		implements
			HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you want to use an attribute value or property of an element for
	 * {@code selected} instead of the index, set this to the name of the
	 * attribute or property. Hyphenated values are converted to camel case when
	 * used to look up the property of a selectable element. Camel cased values
	 * are not* converted to hyphenated values for attribute lookup. It's
	 * recommended that you provide the hyphenated form of the name so that
	 * selection works in both cases. (Use {@code attr-or-property-name} instead
	 * of {@code attrOrPropertyName}.)
	 */
	public String getAttrForSelected() {
		return getElement().getProperty("attrForSelected");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you want to use an attribute value or property of an element for
	 * {@code selected} instead of the index, set this to the name of the
	 * attribute or property. Hyphenated values are converted to camel case when
	 * used to look up the property of a selectable element. Camel cased values
	 * are not* converted to hyphenated values for attribute lookup. It's
	 * recommended that you provide the hyphenated form of the name so that
	 * selection works in both cases. (Use {@code attr-or-property-name} instead
	 * of {@code attrOrPropertyName}.)
	 * 
	 * @param attrForSelected
	 * @return This instance, for method chaining.
	 */
	public R setAttrForSelected(java.lang.String attrForSelected) {
		getElement().setProperty("attrForSelected",
				attrForSelected == null ? "" : attrForSelected);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Gets or sets the selected element. The default is to use the index of the
	 * item.
	 */
	public String getSelectedString() {
		return getElement().getProperty("selected");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Gets or sets the selected element. The default is to use the index of the
	 * item.
	 */
	public double getSelectedNumber() {
		return getElement().getProperty("selected", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Gets or sets the selected element. The default is to use the index of the
	 * item.
	 * 
	 * @param selected
	 * @return This instance, for method chaining.
	 */
	public R setSelected(java.lang.String selected) {
		getElement().setProperty("selected", selected == null ? "" : selected);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Gets or sets the selected element. The default is to use the index of the
	 * item.
	 * 
	 * @param selected
	 * @return This instance, for method chaining.
	 */
	public R setSelected(double selected) {
		getElement().setProperty("selected", selected);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the currently selected item.
	 */
	public JsonObject getSelectedItem() {
		return (JsonObject) getElement().getPropertyRaw("selectedItem");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the currently selected item.
	 * 
	 * @param selectedItem
	 * @return This instance, for method chaining.
	 */
	public R setSelectedItem(elemental.json.JsonObject selectedItem) {
		getElement().setPropertyJson("selectedItem", selectedItem);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The event that fires from items when they are selected. Selectable will
	 * listen for this event from items and update the selection state. Set to
	 * empty string to listen to no events.
	 */
	public String getActivateEvent() {
		return getElement().getProperty("activateEvent");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The event that fires from items when they are selected. Selectable will
	 * listen for this event from items and update the selection state. Set to
	 * empty string to listen to no events.
	 * 
	 * @param activateEvent
	 * @return This instance, for method chaining.
	 */
	public R setActivateEvent(java.lang.String activateEvent) {
		getElement().setProperty("activateEvent",
				activateEvent == null ? "" : activateEvent);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This is a CSS selector string. If this is set, only items that match the
	 * CSS selector are selectable.
	 */
	public String getSelectable() {
		return getElement().getProperty("selectable");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This is a CSS selector string. If this is set, only items that match the
	 * CSS selector are selectable.
	 * 
	 * @param selectable
	 * @return This instance, for method chaining.
	 */
	public R setSelectable(java.lang.String selectable) {
		getElement().setProperty("selectable",
				selectable == null ? "" : selectable);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The class to set on elements when selected.
	 */
	public String getSelectedClass() {
		return getElement().getProperty("selectedClass");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The class to set on elements when selected.
	 * 
	 * @param selectedClass
	 * @return This instance, for method chaining.
	 */
	public R setSelectedClass(java.lang.String selectedClass) {
		getElement().setProperty("selectedClass",
				selectedClass == null ? "" : selectedClass);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The attribute to set on elements when selected.
	 */
	public String getSelectedAttribute() {
		return getElement().getProperty("selectedAttribute");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The attribute to set on elements when selected.
	 * 
	 * @param selectedAttribute
	 * @return This instance, for method chaining.
	 */
	public R setSelectedAttribute(java.lang.String selectedAttribute) {
		getElement().setProperty("selectedAttribute",
				selectedAttribute == null ? "" : selectedAttribute);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Default fallback if the selection based on selected with
	 * {@code attrForSelected} is not found.
	 */
	public String getFallbackSelection() {
		return getElement().getProperty("fallbackSelection");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Default fallback if the selection based on selected with
	 * {@code attrForSelected} is not found.
	 * 
	 * @param fallbackSelection
	 * @return This instance, for method chaining.
	 */
	public R setFallbackSelection(java.lang.String fallbackSelection) {
		getElement().setProperty("fallbackSelection",
				fallbackSelection == null ? "" : fallbackSelection);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The list of items from which a selection can be made.
	 */
	public JsonArray getItems() {
		return (JsonArray) getElement().getPropertyRaw("items");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The list of items from which a selection can be made.
	 * 
	 * @param items
	 * @return This instance, for method chaining.
	 */
	public R setItems(elemental.json.JsonArray items) {
		getElement().setPropertyJson("items", items);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, multiple selections are allowed.
	 */
	public boolean isMulti() {
		return getElement().getProperty("multi", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, multiple selections are allowed.
	 * 
	 * @param multi
	 * @return This instance, for method chaining.
	 */
	public R setMulti(boolean multi) {
		getElement().setProperty("multi", multi);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Gets or sets the selected elements. This is used instead of
	 * {@code selected} when {@code multi} is true.
	 */
	public JsonArray getSelectedValues() {
		return (JsonArray) getElement().getPropertyRaw("selectedValues");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Gets or sets the selected elements. This is used instead of
	 * {@code selected} when {@code multi} is true.
	 * 
	 * @param selectedValues
	 * @return This instance, for method chaining.
	 */
	public R setSelectedValues(elemental.json.JsonArray selectedValues) {
		getElement().setPropertyJson("selectedValues", selectedValues);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns an array of currently selected items.
	 */
	public JsonArray getSelectedItems() {
		return (JsonArray) getElement().getPropertyRaw("selectedItems");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns an array of currently selected items.
	 * 
	 * @param selectedItems
	 * @return This instance, for method chaining.
	 */
	public R setSelectedItems(elemental.json.JsonArray selectedItems) {
		getElement().setPropertyJson("selectedItems", selectedItems);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the index of the given item.
	 * 
	 * @param item
	 * @return It would return a interface elemental.json.JsonObject
	 */
	@NotSupported
	protected void indexOf(elemental.json.JsonObject item) {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Selects the given value. If the {@code multi} property is true, then the
	 * selected state of the {@code value} will be toggled; otherwise the
	 * {@code value} will be selected.
	 * 
	 * @param value
	 *            can be <code>null</code>
	 * @param value
	 *            can be <code>null</code>
	 */
	public void select(java.lang.String valueString, double valueNumber) {
		getElement().callFunction("select", valueString, valueNumber);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Selects the previous item.
	 */
	public void selectPrevious() {
		getElement().callFunction("selectPrevious");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Selects the next item.
	 */
	public void selectNext() {
		getElement().callFunction("selectNext");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Selects the item at the given index.
	 * 
	 * @param index
	 */
	public void selectIndex(elemental.json.JsonObject index) {
		getElement().callFunction("selectIndex", index);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Force a synchronous update of the {@code items} property.
	 * 
	 * NOTE: Consider listening for the {@code iron-items-changed} event to
	 * respond to updates to the set of selectable items after updates to the
	 * DOM list and selection state have been made.
	 * 
	 * WARNING: If you are using this method, you should probably consider an
	 * alternate approach. Synchronously querying for items is potentially slow
	 * for many use cases. The {@code items} property will update asynchronously
	 * on its own to reflect selectable items in the DOM.
	 */
	public void forceSynchronousItemUpdate() {
		getElement().callFunction("forceSynchronousItemUpdate");
	}

	/**
	 * @param multi
	 */
	public void multiChanged(elemental.json.JsonObject multi) {
		getElement().callFunction("multiChanged", multi);
	}

	@DomEvent("iron-activate")
	public static class IronActivateEvent extends ComponentEvent<IronSelector> {
		public IronActivateEvent(IronSelector source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronActivateListener(
			ComponentEventListener<IronActivateEvent> listener) {
		return addListener(IronActivateEvent.class, listener);
	}

	@DomEvent("iron-deselect")
	public static class IronDeselectEvent extends ComponentEvent<IronSelector> {
		public IronDeselectEvent(IronSelector source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronDeselectListener(
			ComponentEventListener<IronDeselectEvent> listener) {
		return addListener(IronDeselectEvent.class, listener);
	}

	@DomEvent("iron-items-changed")
	public static class IronItemsChangedEvent
			extends
				ComponentEvent<IronSelector> {
		public IronItemsChangedEvent(IronSelector source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronItemsChangedListener(
			ComponentEventListener<IronItemsChangedEvent> listener) {
		return addListener(IronItemsChangedEvent.class, listener);
	}

	@DomEvent("iron-select")
	public static class IronSelectEvent extends ComponentEvent<IronSelector> {
		public IronSelectEvent(IronSelector source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronSelectListener(
			ComponentEventListener<IronSelectEvent> listener) {
		return addListener(IronSelectEvent.class, listener);
	}

	@DomEvent("selected-changed")
	public static class SelectedChangedEvent
			extends
				ComponentEvent<IronSelector> {
		public SelectedChangedEvent(IronSelector source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addSelectedChangedListener(
			ComponentEventListener<SelectedChangedEvent> listener) {
		return addListener(SelectedChangedEvent.class, listener);
	}

	@DomEvent("selected-item-changed")
	public static class SelectedItemChangedEvent
			extends
				ComponentEvent<IronSelector> {
		public SelectedItemChangedEvent(IronSelector source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addSelectedItemChangedListener(
			ComponentEventListener<SelectedItemChangedEvent> listener) {
		return addListener(SelectedItemChangedEvent.class, listener);
	}

	@DomEvent("items-changed")
	public static class ItemsChangedEvent extends ComponentEvent<IronSelector> {
		public ItemsChangedEvent(IronSelector source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addItemsChangedListener(
			ComponentEventListener<ItemsChangedEvent> listener) {
		return addListener(ItemsChangedEvent.class, listener);
	}

	@DomEvent("selected-values-changed")
	public static class SelectedValuesChangedEvent
			extends
				ComponentEvent<IronSelector> {
		public SelectedValuesChangedEvent(IronSelector source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addSelectedValuesChangedListener(
			ComponentEventListener<SelectedValuesChangedEvent> listener) {
		return addListener(SelectedValuesChangedEvent.class, listener);
	}

	@DomEvent("selected-items-changed")
	public static class SelectedItemsChangedEvent
			extends
				ComponentEvent<IronSelector> {
		public SelectedItemsChangedEvent(IronSelector source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addSelectedItemsChangedListener(
			ComponentEventListener<SelectedItemsChangedEvent> listener) {
		return addListener(SelectedItemsChangedEvent.class, listener);
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected R getSelf() {
		return (R) this;
	}
}