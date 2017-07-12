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
package com.vaadin.components.iron.list;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.components.iron.list.IronList;
import elemental.json.JsonArray;
import com.vaadin.components.NotSupported;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;
import com.vaadin.ui.HasComponents;

/**
 * 
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.13-SNAPSHOT",
		"WebComponent: iron-list#2.0.4", "Flow#0.1.13-SNAPSHOT"})
@Tag("iron-list")
@HtmlImport("frontend://bower_components/iron-list/iron-list.html")
public class IronList extends Component implements HasStyle, HasComponents {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specifies the element that will handle the scroll event on the behalf of
	 * the current element. This is typically a reference to an element, but
	 * there are a few more posibilities:
	 * 
	 * ### Elements id
	 * 
	 * {@code }`html <div id="scrollable-element" style="overflow: auto;">
	 * <x-element scroll-target="scrollable-element"> <!-- Content-->
	 * </x-element> </div> {@code }` In this case, the {@code scrollTarget} will
	 * point to the outer div element.
	 * 
	 * ### Document scrolling
	 * 
	 * For document scrolling, you can use the reserved word {@code document}:
	 * 
	 * {@code }`html <x-element scroll-target="document"> <!-- Content -->
	 * </x-element> {@code }`
	 * 
	 * ### Elements reference
	 * 
	 * {@code }`js appHeader.scrollTarget =
	 * document.querySelector('#scrollable-element'); {@code }`
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getScrollTarget() {
		return (JsonObject) getElement().getPropertyRaw("scrollTarget");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specifies the element that will handle the scroll event on the behalf of
	 * the current element. This is typically a reference to an element, but
	 * there are a few more posibilities:
	 * 
	 * ### Elements id
	 * 
	 * {@code }`html <div id="scrollable-element" style="overflow: auto;">
	 * <x-element scroll-target="scrollable-element"> <!-- Content-->
	 * </x-element> </div> {@code }` In this case, the {@code scrollTarget} will
	 * point to the outer div element.
	 * 
	 * ### Document scrolling
	 * 
	 * For document scrolling, you can use the reserved word {@code document}:
	 * 
	 * {@code }`html <x-element scroll-target="document"> <!-- Content -->
	 * </x-element> {@code }`
	 * 
	 * ### Elements reference
	 * 
	 * {@code }`js appHeader.scrollTarget =
	 * document.querySelector('#scrollable-element'); {@code }`
	 * 
	 * @param scrollTarget
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public <R extends IronList> R setScrollTarget(
			elemental.json.JsonObject scrollTarget) {
		getElement().setPropertyJson("scrollTarget", scrollTarget);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Instance-level flag for configuring the dirty-checking strategy for this
	 * element. When true, Objects and Arrays will skip dirty checking,
	 * otherwise strict equality checking will be used.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isMutableData() {
		return getElement().getProperty("mutableData", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Instance-level flag for configuring the dirty-checking strategy for this
	 * element. When true, Objects and Arrays will skip dirty checking,
	 * otherwise strict equality checking will be used.
	 * 
	 * @param mutableData
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends IronList> R setMutableData(boolean mutableData) {
		getElement().setProperty("mutableData", mutableData);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An array containing items determining how many instances of the template
	 * to stamp and that that each template instance should bind to.
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
	 * An array containing items determining how many instances of the template
	 * to stamp and that that each template instance should bind to.
	 * 
	 * @param items
	 *            the JsonArray value to set
	 * @return this instance, for method chaining
	 */
	public <R extends IronList> R setItems(elemental.json.JsonArray items) {
		getElement().setPropertyJson("items", items);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The max count of physical items the pool can extend to.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getMaxPhysicalCount() {
		return getElement().getProperty("maxPhysicalCount", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The max count of physical items the pool can extend to.
	 * 
	 * @param maxPhysicalCount
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public <R extends IronList> R setMaxPhysicalCount(double maxPhysicalCount) {
		getElement().setProperty("maxPhysicalCount", maxPhysicalCount);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of the variable to add to the binding scope for the array
	 * element associated with a given template instance.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getAs() {
		return getElement().getProperty("as");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of the variable to add to the binding scope for the array
	 * element associated with a given template instance.
	 * 
	 * @param as
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends IronList> R setAs(java.lang.String as) {
		getElement().setProperty("as", as == null ? "" : as);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of the variable to add to the binding scope with the index for
	 * the row.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getIndexAs() {
		return getElement().getProperty("indexAs");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of the variable to add to the binding scope with the index for
	 * the row.
	 * 
	 * @param indexAs
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends IronList> R setIndexAs(java.lang.String indexAs) {
		getElement().setProperty("indexAs", indexAs == null ? "" : indexAs);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of the variable to add to the binding scope to indicate if the
	 * row is selected.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getSelectedAs() {
		return getElement().getProperty("selectedAs");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of the variable to add to the binding scope to indicate if the
	 * row is selected.
	 * 
	 * @param selectedAs
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends IronList> R setSelectedAs(java.lang.String selectedAs) {
		getElement().setProperty("selectedAs",
				selectedAs == null ? "" : selectedAs);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When true, the list is rendered as a grid. Grid items must have fixed
	 * width and height set via CSS. e.g.
	 * 
	 * {@code }`html <iron-list grid> <template> <div
	 * style="width: 100px; height: 100px;"> 100x100 </div> </template>
	 * </iron-list> {@code }`
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isGrid() {
		return getElement().getProperty("grid", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When true, the list is rendered as a grid. Grid items must have fixed
	 * width and height set via CSS. e.g.
	 * 
	 * {@code }`html <iron-list grid> <template> <div
	 * style="width: 100px; height: 100px;"> 100x100 </div> </template>
	 * </iron-list> {@code }`
	 * 
	 * @param grid
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends IronList> R setGrid(boolean grid) {
		getElement().setProperty("grid", grid);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When true, tapping a row will select the item, placing its data model in
	 * the set of selected items retrievable via the selection property.
	 * 
	 * Note that tapping focusable elements within the list item will not result
	 * in selection, since they are presumed to have their * own action.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isSelectionEnabled() {
		return getElement().getProperty("selectionEnabled", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When true, tapping a row will select the item, placing its data model in
	 * the set of selected items retrievable via the selection property.
	 * 
	 * Note that tapping focusable elements within the list item will not result
	 * in selection, since they are presumed to have their * own action.
	 * 
	 * @param selectionEnabled
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends IronList> R setSelectionEnabled(boolean selectionEnabled) {
		getElement().setProperty("selectionEnabled", selectionEnabled);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When {@code multiSelection} is false, this is the currently selected
	 * item, or {@code null} if no item is selected.
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
	 * When {@code multiSelection} is false, this is the currently selected
	 * item, or {@code null} if no item is selected.
	 * 
	 * @param selectedItem
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public <R extends IronList> R setSelectedItem(
			elemental.json.JsonObject selectedItem) {
		getElement().setPropertyJson("selectedItem", selectedItem);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When {@code multiSelection} is true, this is an array that contains the
	 * selected items.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getSelectedItems() {
		return (JsonObject) getElement().getPropertyRaw("selectedItems");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When {@code multiSelection} is true, this is an array that contains the
	 * selected items.
	 * 
	 * @param selectedItems
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public <R extends IronList> R setSelectedItems(
			elemental.json.JsonObject selectedItems) {
		getElement().setPropertyJson("selectedItems", selectedItems);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When {@code true}, multiple items may be selected at once (in this case,
	 * {@code selected} is an array of currently selected items). When
	 * {@code false}, only one item may be selected at a time.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isMultiSelection() {
		return getElement().getProperty("multiSelection", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When {@code true}, multiple items may be selected at once (in this case,
	 * {@code selected} is an array of currently selected items). When
	 * {@code false}, only one item may be selected at a time.
	 * 
	 * @param multiSelection
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends IronList> R setMultiSelection(boolean multiSelection) {
		getElement().setProperty("multiSelection", multiSelection);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The offset top from the scrolling element to the iron-list element. This
	 * value can be computed using the position returned by
	 * {@code getBoundingClientRect()} although it's preferred to use a constant
	 * value when possible.
	 * 
	 * This property is useful when an external scrolling element is used and
	 * there's some offset between the scrolling element and the list. For
	 * example: a header is placed above the list.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getScrollOffset() {
		return getElement().getProperty("scrollOffset", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The offset top from the scrolling element to the iron-list element. This
	 * value can be computed using the position returned by
	 * {@code getBoundingClientRect()} although it's preferred to use a constant
	 * value when possible.
	 * 
	 * This property is useful when an external scrolling element is used and
	 * there's some offset between the scrolling element and the list. For
	 * example: a header is placed above the list.
	 * 
	 * @param scrollOffset
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public <R extends IronList> R setScrollOffset(double scrollOffset) {
		getElement().setProperty("scrollOffset", scrollOffset);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Gets the index of the first visible item in the viewport.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getFirstVisibleIndex() {
		return getElement().getProperty("firstVisibleIndex", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Gets the index of the first visible item in the viewport.
	 * 
	 * @param firstVisibleIndex
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public <R extends IronList> R setFirstVisibleIndex(double firstVisibleIndex) {
		getElement().setProperty("firstVisibleIndex", firstVisibleIndex);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Gets the index of the last visible item in the viewport.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getLastVisibleIndex() {
		return getElement().getProperty("lastVisibleIndex", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Gets the index of the last visible item in the viewport.
	 * 
	 * @param lastVisibleIndex
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public <R extends IronList> R setLastVisibleIndex(double lastVisibleIndex) {
		getElement().setProperty("lastVisibleIndex", lastVisibleIndex);
		return getSelf();
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
	 * Can be called to manually notify a resizable and its descendant
	 * resizables of a resize change.
	 */
	public void notifyResize() {
		getElement().callFunction("notifyResize");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Used to assign the closest resizable ancestor to this resizable if the
	 * ancestor detects a request for notifications.
	 * 
	 * @param parentResizable
	 *            Missing documentation!
	 */
	public void assignParentResizable(elemental.json.JsonObject parentResizable) {
		getElement().callFunction("assignParentResizable", parentResizable);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Used to remove a resizable descendant from the list of descendants that
	 * should be notified of a resize change.
	 * 
	 * @param target
	 *            Missing documentation!
	 */
	public void stopResizeNotificationsFor(elemental.json.JsonObject target) {
		getElement().callFunction("stopResizeNotificationsFor", target);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This method can be overridden to filter nested elements that should or
	 * should not be notified by the current element. Return true if an element
	 * should be notified, or false if it should not be notified.
	 * 
	 * @param element
	 *            A candidate descendant element that implements
	 *            `IronResizableBehavior`.
	 * @return It would return a boolean
	 */
	@NotSupported
	protected void resizerShouldNotify(elemental.json.JsonObject element) {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Scrolls the content to a particular place.
	 * 
	 * @param left
	 *            The left position
	 * @param top
	 *            The top position
	 */
	public void scroll(double left, double top) {
		getElement().callFunction("scroll", left, top);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Enables or disables the scroll event listener.
	 * 
	 * @param yes
	 *            True to add the event, False to remove it.
	 */
	public void toggleScrollListener(boolean yes) {
		getElement().callFunction("toggleScrollListener", yes);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Invoke this method if you dynamically update the viewport's size or CSS
	 * padding.
	 */
	public void updateViewportBoundaries() {
		getElement().callFunction("updateViewportBoundaries");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Scroll to a specific item in the virtual list regardless of the physical
	 * items in the DOM tree.
	 * 
	 * @param item
	 *            The item to be scrolled to
	 */
	public void scrollToItem(elemental.json.JsonObject item) {
		getElement().callFunction("scrollToItem", item);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Scroll to a specific index in the virtual list regardless of the physical
	 * items in the DOM tree.
	 * 
	 * @param idx
	 *            The index of the item
	 */
	public void scrollToIndex(double idx) {
		getElement().callFunction("scrollToIndex", idx);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Selects the given item.
	 * 
	 * @param item
	 *            The item instance.
	 */
	public void selectItem(elemental.json.JsonObject item) {
		getElement().callFunction("selectItem", item);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Selects the item at the given index in the items array.
	 * 
	 * @param index
	 *            The item instance.
	 */
	public void selectIndex(elemental.json.JsonObject index) {
		getElement().callFunction("selectIndex", index);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Deselects the given item.
	 * 
	 * @param item
	 *            The item instance.
	 */
	public void deselectItem(elemental.json.JsonObject item) {
		getElement().callFunction("deselectItem", item);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Deselects the item at the given index in the items array.
	 * 
	 * @param index
	 *            The index of the item in the items array.
	 */
	public void deselectIndex(double index) {
		getElement().callFunction("deselectIndex", index);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Selects or deselects a given item depending on whether the item has
	 * already been selected.
	 * 
	 * @param item
	 *            The item object.
	 */
	public void toggleSelectionForItem(elemental.json.JsonObject item) {
		getElement().callFunction("toggleSelectionForItem", item);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Selects or deselects the item at the given index in the items array
	 * depending on whether the item has already been selected.
	 * 
	 * @param index
	 *            The index of the item in the items array.
	 */
	public void toggleSelectionForIndex(elemental.json.JsonObject index) {
		getElement().callFunction("toggleSelectionForIndex", index);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Clears the current selection in the list.
	 */
	public void clearSelection() {
		getElement().callFunction("clearSelection");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Updates the size of a given list item.
	 * 
	 * @param item
	 *            The item instance.
	 */
	public void updateSizeForItem(elemental.json.JsonObject item) {
		getElement().callFunction("updateSizeForItem", item);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Updates the size of the item at the given index in the items array.
	 * 
	 * @param index
	 *            The index of the item in the items array.
	 */
	public void updateSizeForIndex(double index) {
		getElement().callFunction("updateSizeForIndex", index);
	}

	/**
	 * @param idx
	 *            Missing documentation!
	 */
	public void focusItem(elemental.json.JsonObject idx) {
		getElement().callFunction("focusItem", idx);
	}

	@DomEvent("selected-item-changed")
	public static class SelectedItemChangedEvent
			extends
				ComponentEvent<IronList> {
		public SelectedItemChangedEvent(IronList source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addSelectedItemChangedListener(
			ComponentEventListener<SelectedItemChangedEvent> listener) {
		return addListener(SelectedItemChangedEvent.class, listener);
	}

	@DomEvent("selected-items-changed")
	public static class SelectedItemsChangedEvent
			extends
				ComponentEvent<IronList> {
		public SelectedItemsChangedEvent(IronList source, boolean fromClient) {
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
	protected <R extends IronList> R getSelf() {
		return (R) this;
	}
}