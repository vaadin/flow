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
package com.vaadin.components.paper.tabs;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Synchronize;
import elemental.json.JsonObject;
import elemental.json.JsonArray;
import com.vaadin.components.NotSupported;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;
import com.vaadin.ui.HasComponents;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * Material design:
 * [Tabs](https://www.google.com/design/spec/components/tabs.html)
 * 
 * {@code paper-tabs} makes it easy to explore and switch between different
 * views or functional aspects of an app, or to browse categorized data sets.
 * 
 * Use {@code selected} property to get or set the selected tab.
 * 
 * Example:
 * 
 * <paper-tabs selected="0"> <paper-tab>TAB 1</paper-tab> <paper-tab>TAB
 * 2</paper-tab> <paper-tab>TAB 3</paper-tab> </paper-tabs>
 * 
 * See <a href="?active=paper-tab">paper-tab</a> for more information about
 * {@code paper-tab}.
 * 
 * A common usage for {@code paper-tabs} is to use it along with
 * {@code iron-pages} to switch between different views.
 * 
 * <paper-tabs selected="{{selected}}"> <paper-tab>Tab 1</paper-tab>
 * <paper-tab>Tab 2</paper-tab> <paper-tab>Tab 3</paper-tab> </paper-tabs>
 * 
 * <iron-pages selected="{{selected}}"> <div>Page 1</div> <div>Page 2</div>
 * <div>Page 3</div> </iron-pages>
 * 
 * 
 * To use links in tabs, add {@code link} attribute to {@code paper-tab} and put
 * an {@code <a>} element in {@code paper-tab} with a {@code tabindex} of -1.
 * 
 * Example:
 * 
 * <pre>
 * <code>
 * &lt;style is="custom-style">
 *   .link {
 *     &#64;apply --layout-horizontal;
 *     &#64;apply --layout-center-center;
 *   }
 * &lt;/style>
 * 
 * &lt;paper-tabs selected="0">
 *   &lt;paper-tab link>
 *     &lt;a href="#link1" class="link" tabindex="-1">TAB ONE&lt;/a>
 *   &lt;/paper-tab>
 *   &lt;paper-tab link>
 *     &lt;a href="#link2" class="link" tabindex="-1">TAB TWO&lt;/a>
 *   &lt;/paper-tab>
 *   &lt;paper-tab link>
 *     &lt;a href="#link3" class="link" tabindex="-1">TAB THREE&lt;/a>
 *   &lt;/paper-tab>
 * &lt;/paper-tabs>
 * </code>
 * </pre>
 * 
 * ### Styling
 * 
 * The following custom properties and mixins are available for styling:
 * 
 * Custom property | Description | Default
 * ----------------|-------------|----------
 * {@code --paper-tabs-selection-bar-color} | Color for the selection bar |
 * {@code --paper-yellow-a100} {@code --paper-tabs-selection-bar} | Mixin
 * applied to the selection bar | {@code {@code --paper-tabs} | Mixin applied
 * to the tabs | {@code {@code --paper-tabs-content} | Mixin applied to the
 * content container of tabs | {@code {@code --paper-tabs-container} | Mixin
 * applied to the layout container of tabs | {@code
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.12-SNAPSHOT",
		"WebComponent: paper-tabs#2.0.0", "Flow#0.1.12-SNAPSHOT"})
@Tag("paper-tabs")
@HtmlImport("frontend://bower_components/paper-tabs/paper-tabs.html")
public class PaperTabs<R extends PaperTabs<R>> extends Component
		implements
			HasStyle,
			HasComponents {

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
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 * @return this instance, for method chaining
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
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'selected-changed' event happens.
	 */
	@Synchronize(property = "selected", value = "selected-changed")
	public String getSelectedString() {
		return getElement().getProperty("selected");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Gets or sets the selected element. The default is to use the index of the
	 * item.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'selected-changed' event happens.
	 */
	@Synchronize(property = "selected", value = "selected-changed")
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
	 * @return this instance, for method chaining
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
	 * @return this instance, for method chaining
	 */
	public R setSelected(double selected) {
		getElement().setProperty("selected", selected);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the currently selected item.
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
	 * Returns the currently selected item.
	 * 
	 * @param selectedItem
	 * @return this instance, for method chaining
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
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 * @return this instance, for method chaining
	 */
	public R setActivateEvent(java.lang.String activateEvent) {
		getElement().setProperty("activateEvent",
				activateEvent == null ? "" : activateEvent);
		return getSelf();
	}

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getSelectable() {
		return getElement().getProperty("selectable");
	}

	/**
	 * @param selectable
	 * @return this instance, for method chaining
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
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 * @return this instance, for method chaining
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
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 * @return this instance, for method chaining
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
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 * @return this instance, for method chaining
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
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'items-changed' event happens.
	 */
	@Synchronize(property = "items", value = "items-changed")
	public JsonArray getItems() {
		return (JsonArray) getElement().getPropertyRaw("items");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The list of items from which a selection can be made.
	 * 
	 * @param items
	 * @return this instance, for method chaining
	 */
	public R setItems(elemental.json.JsonArray items) {
		getElement().setPropertyJson("items", items);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, multiple selections are allowed.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 * @return this instance, for method chaining
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
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 * @return this instance, for method chaining
	 */
	public R setSelectedValues(elemental.json.JsonArray selectedValues) {
		getElement().setPropertyJson("selectedValues", selectedValues);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns an array of currently selected items.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 * @return this instance, for method chaining
	 */
	public R setSelectedItems(elemental.json.JsonArray selectedItems) {
		getElement().setPropertyJson("selectedItems", selectedItems);
		return getSelf();
	}

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
	 * @return this instance, for method chaining
	 */
	public R setKeyEventTarget(elemental.json.JsonObject keyEventTarget) {
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
	 * @return this instance, for method chaining
	 */
	public R setStopKeyboardEventPropagation(
			boolean stopKeyboardEventPropagation) {
		getElement().setProperty("stopKeyboardEventPropagation",
				stopKeyboardEventPropagation);
		return getSelf();
	}

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getKeyBindings() {
		return (JsonObject) getElement().getPropertyRaw("keyBindings");
	}

	/**
	 * @param keyBindings
	 * @return this instance, for method chaining
	 */
	public R setKeyBindings(elemental.json.JsonObject keyBindings) {
		getElement().setPropertyJson("keyBindings", keyBindings);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the currently focused item.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getFocusedItem() {
		return (JsonObject) getElement().getPropertyRaw("focusedItem");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the currently focused item.
	 * 
	 * @param focusedItem
	 * @return this instance, for method chaining
	 */
	public R setFocusedItem(elemental.json.JsonObject focusedItem) {
		getElement().setPropertyJson("focusedItem", focusedItem);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The attribute to use on menu items to look up the item title. Typing the
	 * first letter of an item when the menu is open focuses that item. If
	 * unset, {@code textContent} will be used.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getAttrForItemTitle() {
		return getElement().getProperty("attrForItemTitle");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The attribute to use on menu items to look up the item title. Typing the
	 * first letter of an item when the menu is open focuses that item. If
	 * unset, {@code textContent} will be used.
	 * 
	 * @param attrForItemTitle
	 * @return this instance, for method chaining
	 */
	public R setAttrForItemTitle(java.lang.String attrForItemTitle) {
		getElement().setProperty("attrForItemTitle",
				attrForItemTitle == null ? "" : attrForItemTitle);
		return getSelf();
	}

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isDisabled() {
		return getElement().getProperty("disabled", false);
	}

	/**
	 * @param disabled
	 * @return this instance, for method chaining
	 */
	public R setDisabled(boolean disabled) {
		getElement().setProperty("disabled", disabled);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, ink ripple effect is disabled. When this property is changed,
	 * all descendant {@code <paper-tab>} elements have their {@code noink}
	 * property changed to the new value as well.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isNoink() {
		return getElement().getProperty("noink", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, ink ripple effect is disabled. When this property is changed,
	 * all descendant {@code <paper-tab>} elements have their {@code noink}
	 * property changed to the new value as well.
	 * 
	 * @param noink
	 * @return this instance, for method chaining
	 */
	public R setNoink(boolean noink) {
		getElement().setProperty("noink", noink);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the bottom bar to indicate the selected tab will not be shown.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isNoBar() {
		return getElement().getProperty("noBar", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the bottom bar to indicate the selected tab will not be shown.
	 * 
	 * @param noBar
	 * @return this instance, for method chaining
	 */
	public R setNoBar(boolean noBar) {
		getElement().setProperty("noBar", noBar);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the slide effect for the bottom bar is disabled.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isNoSlide() {
		return getElement().getProperty("noSlide", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the slide effect for the bottom bar is disabled.
	 * 
	 * @param noSlide
	 * @return this instance, for method chaining
	 */
	public R setNoSlide(boolean noSlide) {
		getElement().setProperty("noSlide", noSlide);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, tabs are scrollable and the tab width is based on the label
	 * width.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isScrollable() {
		return getElement().getProperty("scrollable", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, tabs are scrollable and the tab width is based on the label
	 * width.
	 * 
	 * @param scrollable
	 * @return this instance, for method chaining
	 */
	public R setScrollable(boolean scrollable) {
		getElement().setProperty("scrollable", scrollable);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, tabs expand to fit their container. This currently only applies
	 * when scrollable is true.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isFitContainer() {
		return getElement().getProperty("fitContainer", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, tabs expand to fit their container. This currently only applies
	 * when scrollable is true.
	 * 
	 * @param fitContainer
	 * @return this instance, for method chaining
	 */
	public R setFitContainer(boolean fitContainer) {
		getElement().setProperty("fitContainer", fitContainer);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, dragging on the tabs to scroll is disabled.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isDisableDrag() {
		return getElement().getProperty("disableDrag", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, dragging on the tabs to scroll is disabled.
	 * 
	 * @param disableDrag
	 * @return this instance, for method chaining
	 */
	public R setDisableDrag(boolean disableDrag) {
		getElement().setProperty("disableDrag", disableDrag);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, scroll buttons (left/right arrow) will be hidden for scrollable
	 * tabs.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isHideScrollButtons() {
		return getElement().getProperty("hideScrollButtons", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, scroll buttons (left/right arrow) will be hidden for scrollable
	 * tabs.
	 * 
	 * @param hideScrollButtons
	 * @return this instance, for method chaining
	 */
	public R setHideScrollButtons(boolean hideScrollButtons) {
		getElement().setProperty("hideScrollButtons", hideScrollButtons);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the tabs are aligned to bottom (the selection bar appears at the
	 * top).
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isAlignBottom() {
		return getElement().getProperty("alignBottom", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the tabs are aligned to bottom (the selection bar appears at the
	 * top).
	 * 
	 * @param alignBottom
	 * @return this instance, for method chaining
	 */
	public R setAlignBottom(boolean alignBottom) {
		getElement().setProperty("alignBottom", alignBottom);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, tabs are automatically selected when focused using the keyboard.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isAutoselect() {
		return getElement().getProperty("autoselect", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, tabs are automatically selected when focused using the keyboard.
	 * 
	 * @param autoselect
	 * @return this instance, for method chaining
	 */
	public R setAutoselect(boolean autoselect) {
		getElement().setProperty("autoselect", autoselect);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The delay (in milliseconds) between when the user stops interacting with
	 * the tabs through the keyboard and when the focused item is automatically
	 * selected (if {@code autoselect} is true).
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getAutoselectDelay() {
		return getElement().getProperty("autoselectDelay", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The delay (in milliseconds) between when the user stops interacting with
	 * the tabs through the keyboard and when the focused item is automatically
	 * selected (if {@code autoselect} is true).
	 * 
	 * @param autoselectDelay
	 * @return this instance, for method chaining
	 */
	public R setAutoselectDelay(double autoselectDelay) {
		getElement().setProperty("autoselectDelay", autoselectDelay);
		return getSelf();
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
	 * @return It would return a boolean
	 */
	@NotSupported
	protected void resizerShouldNotify(elemental.json.JsonObject element) {
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

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Can be used to imperatively add a key binding to the implementing
	 * element. This is the imperative equivalent of declaring a keybinding in
	 * the {@code keyBindings} prototype property.
	 * 
	 * @param eventString
	 * @param handlerName
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
	 * @param eventString
	 * @return It would return a boolean
	 */
	@NotSupported
	protected void keyboardEventMatchesKeys(elemental.json.JsonObject event,
			java.lang.String eventString) {
	}

	@DomEvent("iron-activate")
	public static class IronActivateEvent extends ComponentEvent<PaperTabs> {
		public IronActivateEvent(PaperTabs source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronActivateListener(
			ComponentEventListener<IronActivateEvent> listener) {
		return addListener(IronActivateEvent.class, listener);
	}

	@DomEvent("iron-deselect")
	public static class IronDeselectEvent extends ComponentEvent<PaperTabs> {
		public IronDeselectEvent(PaperTabs source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronDeselectListener(
			ComponentEventListener<IronDeselectEvent> listener) {
		return addListener(IronDeselectEvent.class, listener);
	}

	@DomEvent("iron-items-changed")
	public static class IronItemsChangedEvent extends ComponentEvent<PaperTabs> {
		public IronItemsChangedEvent(PaperTabs source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronItemsChangedListener(
			ComponentEventListener<IronItemsChangedEvent> listener) {
		return addListener(IronItemsChangedEvent.class, listener);
	}

	@DomEvent("iron-select")
	public static class IronSelectEvent extends ComponentEvent<PaperTabs> {
		public IronSelectEvent(PaperTabs source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronSelectListener(
			ComponentEventListener<IronSelectEvent> listener) {
		return addListener(IronSelectEvent.class, listener);
	}

	@DomEvent("selected-changed")
	public static class SelectedChangedEvent extends ComponentEvent<PaperTabs> {
		public SelectedChangedEvent(PaperTabs source, boolean fromClient) {
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
				ComponentEvent<PaperTabs> {
		public SelectedItemChangedEvent(PaperTabs source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addSelectedItemChangedListener(
			ComponentEventListener<SelectedItemChangedEvent> listener) {
		return addListener(SelectedItemChangedEvent.class, listener);
	}

	@DomEvent("items-changed")
	public static class ItemsChangedEvent extends ComponentEvent<PaperTabs> {
		public ItemsChangedEvent(PaperTabs source, boolean fromClient) {
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
				ComponentEvent<PaperTabs> {
		public SelectedValuesChangedEvent(PaperTabs source, boolean fromClient) {
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
				ComponentEvent<PaperTabs> {
		public SelectedItemsChangedEvent(PaperTabs source, boolean fromClient) {
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