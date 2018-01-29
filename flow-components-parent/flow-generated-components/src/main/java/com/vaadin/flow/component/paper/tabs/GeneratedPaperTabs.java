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
package com.vaadin.flow.component.paper.tabs;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.ComponentSupplier;
import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.Synchronize;
import elemental.json.JsonObject;
import elemental.json.JsonArray;
import com.vaadin.flow.component.NotSupported;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;

@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: paper-tabs#2.0.0", "Flow#1.0-SNAPSHOT" })
@Tag("paper-tabs")
@HtmlImport("frontend://bower_components/paper-tabs/paper-tabs.html")
public abstract class GeneratedPaperTabs<R extends GeneratedPaperTabs<R>>
        extends Component implements HasStyle, ComponentSupplier<R> {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you want to use an attribute value or property of an element for
     * {@code selected} instead of the index, set this to the name of the
     * attribute or property. Hyphenated values are converted to camel case when
     * used to look up the property of a selectable element. Camel cased values
     * are <em>not</em> converted to hyphenated values for attribute lookup.
     * It's recommended that you provide the hyphenated form of the name so that
     * selection works in both cases. (Use {@code attr-or-property-name} instead
     * of {@code attrOrPropertyName}.)
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code attrForSelected} property from the webcomponent
     */
    protected String getAttrForSelectedString() {
        return getElement().getProperty("attrForSelected");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you want to use an attribute value or property of an element for
     * {@code selected} instead of the index, set this to the name of the
     * attribute or property. Hyphenated values are converted to camel case when
     * used to look up the property of a selectable element. Camel cased values
     * are <em>not</em> converted to hyphenated values for attribute lookup.
     * It's recommended that you provide the hyphenated form of the name so that
     * selection works in both cases. (Use {@code attr-or-property-name} instead
     * of {@code attrOrPropertyName}.)
     * </p>
     * 
     * @param attrForSelected
     *            the String value to set
     */
    protected void setAttrForSelected(String attrForSelected) {
        getElement().setProperty("attrForSelected",
                attrForSelected == null ? "" : attrForSelected);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Gets or sets the selected element. The default is to use the index of the
     * item.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'selected-changed' event happens.
     * </p>
     * 
     * @return the {@code selected} property from the webcomponent
     */
    @Synchronize(property = "selected", value = "selected-changed")
    protected double getSelectedNumberDouble() {
        return getElement().getProperty("selected", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Gets or sets the selected element. The default is to use the index of the
     * item.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'selected-changed' event happens.
     * </p>
     * 
     * @return the {@code selected} property from the webcomponent
     */
    @Synchronize(property = "selected", value = "selected-changed")
    protected String getSelectedStringString() {
        return getElement().getProperty("selected");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Gets or sets the selected element. The default is to use the index of the
     * item.
     * </p>
     * 
     * @param selected
     *            the double value to set
     */
    protected void setSelected(double selected) {
        getElement().setProperty("selected", selected);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Gets or sets the selected element. The default is to use the index of the
     * item.
     * </p>
     * 
     * @param selected
     *            the String value to set
     */
    protected void setSelected(String selected) {
        getElement().setProperty("selected", selected == null ? "" : selected);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns the currently selected item.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code selectedItem} property from the webcomponent
     */
    protected JsonObject getSelectedItemJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("selectedItem");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The event that fires from items when they are selected. Selectable will
     * listen for this event from items and update the selection state. Set to
     * empty string to listen to no events.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code activateEvent} property from the webcomponent
     */
    protected String getActivateEventString() {
        return getElement().getProperty("activateEvent");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The event that fires from items when they are selected. Selectable will
     * listen for this event from items and update the selection state. Set to
     * empty string to listen to no events.
     * </p>
     * 
     * @param activateEvent
     *            the String value to set
     */
    protected void setActivateEvent(String activateEvent) {
        getElement().setProperty("activateEvent",
                activateEvent == null ? "" : activateEvent);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code selectable} property from the webcomponent
     */
    protected String getSelectableString() {
        return getElement().getProperty("selectable");
    }

    /**
     * @param selectable
     *            the String value to set
     */
    protected void setSelectable(String selectable) {
        getElement().setProperty("selectable",
                selectable == null ? "" : selectable);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The class to set on elements when selected.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code selectedClass} property from the webcomponent
     */
    protected String getSelectedClassString() {
        return getElement().getProperty("selectedClass");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The class to set on elements when selected.
     * </p>
     * 
     * @param selectedClass
     *            the String value to set
     */
    protected void setSelectedClass(String selectedClass) {
        getElement().setProperty("selectedClass",
                selectedClass == null ? "" : selectedClass);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The attribute to set on elements when selected.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code selectedAttribute} property from the webcomponent
     */
    protected String getSelectedAttributeString() {
        return getElement().getProperty("selectedAttribute");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The attribute to set on elements when selected.
     * </p>
     * 
     * @param selectedAttribute
     *            the String value to set
     */
    protected void setSelectedAttribute(String selectedAttribute) {
        getElement().setProperty("selectedAttribute",
                selectedAttribute == null ? "" : selectedAttribute);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Default fallback if the selection based on selected with
     * {@code attrForSelected} is not found.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code fallbackSelection} property from the webcomponent
     */
    protected String getFallbackSelectionString() {
        return getElement().getProperty("fallbackSelection");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Default fallback if the selection based on selected with
     * {@code attrForSelected} is not found.
     * </p>
     * 
     * @param fallbackSelection
     *            the String value to set
     */
    protected void setFallbackSelection(String fallbackSelection) {
        getElement().setProperty("fallbackSelection",
                fallbackSelection == null ? "" : fallbackSelection);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The list of items from which a selection can be made.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'items-changed' event happens.
     * </p>
     * 
     * @return the {@code items} property from the webcomponent
     */
    @Synchronize(property = "items", value = "items-changed")
    protected JsonArray getItemsJsonArray() {
        return (JsonArray) getElement().getPropertyRaw("items");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, multiple selections are allowed.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code multi} property from the webcomponent
     */
    protected boolean isMultiBoolean() {
        return getElement().getProperty("multi", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, multiple selections are allowed.
     * </p>
     * 
     * @param multi
     *            the boolean value to set
     */
    protected void setMulti(boolean multi) {
        getElement().setProperty("multi", multi);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Gets or sets the selected elements. This is used instead of
     * {@code selected} when {@code multi} is true.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code selectedValues} property from the webcomponent
     */
    protected JsonArray getSelectedValuesJsonArray() {
        return (JsonArray) getElement().getPropertyRaw("selectedValues");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Gets or sets the selected elements. This is used instead of
     * {@code selected} when {@code multi} is true.
     * </p>
     * 
     * @param selectedValues
     *            the JsonArray value to set
     */
    protected void setSelectedValues(JsonArray selectedValues) {
        getElement().setPropertyJson("selectedValues", selectedValues);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns an array of currently selected items.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code selectedItems} property from the webcomponent
     */
    protected JsonArray getSelectedItemsJsonArray() {
        return (JsonArray) getElement().getPropertyRaw("selectedItems");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The EventTarget that will be firing relevant KeyboardEvents. Set it to
     * {@code null} to disable the listeners.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code keyEventTarget} property from the webcomponent
     */
    protected JsonObject getKeyEventTargetJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("keyEventTarget");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The EventTarget that will be firing relevant KeyboardEvents. Set it to
     * {@code null} to disable the listeners.
     * </p>
     * 
     * @param keyEventTarget
     *            the JsonObject value to set
     */
    protected void setKeyEventTarget(JsonObject keyEventTarget) {
        getElement().setPropertyJson("keyEventTarget", keyEventTarget);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, this property will cause the implementing element to
     * automatically stop propagation on any handled KeyboardEvents.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code stopKeyboardEventPropagation} property from the
     *         webcomponent
     */
    protected boolean isStopKeyboardEventPropagationBoolean() {
        return getElement().getProperty("stopKeyboardEventPropagation", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, this property will cause the implementing element to
     * automatically stop propagation on any handled KeyboardEvents.
     * </p>
     * 
     * @param stopKeyboardEventPropagation
     *            the boolean value to set
     */
    protected void setStopKeyboardEventPropagation(
            boolean stopKeyboardEventPropagation) {
        getElement().setProperty("stopKeyboardEventPropagation",
                stopKeyboardEventPropagation);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code keyBindings} property from the webcomponent
     */
    protected JsonObject getKeyBindingsJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("keyBindings");
    }

    /**
     * @param keyBindings
     *            the JsonObject value to set
     */
    protected void setKeyBindings(JsonObject keyBindings) {
        getElement().setPropertyJson("keyBindings", keyBindings);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns the currently focused item.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code focusedItem} property from the webcomponent
     */
    protected JsonObject getFocusedItemJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("focusedItem");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The attribute to use on menu items to look up the item title. Typing the
     * first letter of an item when the menu is open focuses that item. If
     * unset, {@code textContent} will be used.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code attrForItemTitle} property from the webcomponent
     */
    protected String getAttrForItemTitleString() {
        return getElement().getProperty("attrForItemTitle");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The attribute to use on menu items to look up the item title. Typing the
     * first letter of an item when the menu is open focuses that item. If
     * unset, {@code textContent} will be used.
     * </p>
     * 
     * @param attrForItemTitle
     *            the String value to set
     */
    protected void setAttrForItemTitle(String attrForItemTitle) {
        getElement().setProperty("attrForItemTitle",
                attrForItemTitle == null ? "" : attrForItemTitle);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code disabled} property from the webcomponent
     */
    protected boolean isDisabledBoolean() {
        return getElement().getProperty("disabled", false);
    }

    /**
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
     * If true, ink ripple effect is disabled. When this property is changed,
     * all descendant {@code <paper-tab>} elements have their {@code noink}
     * property changed to the new value as well.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code noink} property from the webcomponent
     */
    protected boolean isNoinkBoolean() {
        return getElement().getProperty("noink", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, ink ripple effect is disabled. When this property is changed,
     * all descendant {@code <paper-tab>} elements have their {@code noink}
     * property changed to the new value as well.
     * </p>
     * 
     * @param noink
     *            the boolean value to set
     */
    protected void setNoink(boolean noink) {
        getElement().setProperty("noink", noink);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the bottom bar to indicate the selected tab will not be shown.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code noBar} property from the webcomponent
     */
    protected boolean isNoBarBoolean() {
        return getElement().getProperty("noBar", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the bottom bar to indicate the selected tab will not be shown.
     * </p>
     * 
     * @param noBar
     *            the boolean value to set
     */
    protected void setNoBar(boolean noBar) {
        getElement().setProperty("noBar", noBar);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the slide effect for the bottom bar is disabled.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code noSlide} property from the webcomponent
     */
    protected boolean isNoSlideBoolean() {
        return getElement().getProperty("noSlide", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the slide effect for the bottom bar is disabled.
     * </p>
     * 
     * @param noSlide
     *            the boolean value to set
     */
    protected void setNoSlide(boolean noSlide) {
        getElement().setProperty("noSlide", noSlide);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, tabs are scrollable and the tab width is based on the label
     * width.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code scrollable} property from the webcomponent
     */
    protected boolean isScrollableBoolean() {
        return getElement().getProperty("scrollable", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, tabs are scrollable and the tab width is based on the label
     * width.
     * </p>
     * 
     * @param scrollable
     *            the boolean value to set
     */
    protected void setScrollable(boolean scrollable) {
        getElement().setProperty("scrollable", scrollable);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, tabs expand to fit their container. This currently only applies
     * when scrollable is true.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code fitContainer} property from the webcomponent
     */
    protected boolean isFitContainerBoolean() {
        return getElement().getProperty("fitContainer", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, tabs expand to fit their container. This currently only applies
     * when scrollable is true.
     * </p>
     * 
     * @param fitContainer
     *            the boolean value to set
     */
    protected void setFitContainer(boolean fitContainer) {
        getElement().setProperty("fitContainer", fitContainer);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, dragging on the tabs to scroll is disabled.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code disableDrag} property from the webcomponent
     */
    protected boolean isDisableDragBoolean() {
        return getElement().getProperty("disableDrag", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, dragging on the tabs to scroll is disabled.
     * </p>
     * 
     * @param disableDrag
     *            the boolean value to set
     */
    protected void setDisableDrag(boolean disableDrag) {
        getElement().setProperty("disableDrag", disableDrag);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, scroll buttons (left/right arrow) will be hidden for scrollable
     * tabs.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code hideScrollButtons} property from the webcomponent
     */
    protected boolean isHideScrollButtonsBoolean() {
        return getElement().getProperty("hideScrollButtons", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, scroll buttons (left/right arrow) will be hidden for scrollable
     * tabs.
     * </p>
     * 
     * @param hideScrollButtons
     *            the boolean value to set
     */
    protected void setHideScrollButtons(boolean hideScrollButtons) {
        getElement().setProperty("hideScrollButtons", hideScrollButtons);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the tabs are aligned to bottom (the selection bar appears at the
     * top).
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code alignBottom} property from the webcomponent
     */
    protected boolean isAlignBottomBoolean() {
        return getElement().getProperty("alignBottom", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the tabs are aligned to bottom (the selection bar appears at the
     * top).
     * </p>
     * 
     * @param alignBottom
     *            the boolean value to set
     */
    protected void setAlignBottom(boolean alignBottom) {
        getElement().setProperty("alignBottom", alignBottom);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, tabs are automatically selected when focused using the keyboard.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code autoselect} property from the webcomponent
     */
    protected boolean isAutoselectBoolean() {
        return getElement().getProperty("autoselect", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, tabs are automatically selected when focused using the keyboard.
     * </p>
     * 
     * @param autoselect
     *            the boolean value to set
     */
    protected void setAutoselect(boolean autoselect) {
        getElement().setProperty("autoselect", autoselect);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The delay (in milliseconds) between when the user stops interacting with
     * the tabs through the keyboard and when the focused item is automatically
     * selected (if {@code autoselect} is true).
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code autoselectDelay} property from the webcomponent
     */
    protected double getAutoselectDelayDouble() {
        return getElement().getProperty("autoselectDelay", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The delay (in milliseconds) between when the user stops interacting with
     * the tabs through the keyboard and when the focused item is automatically
     * selected (if {@code autoselect} is true).
     * </p>
     * 
     * @param autoselectDelay
     *            the double value to set
     */
    protected void setAutoselectDelay(double autoselectDelay) {
        getElement().setProperty("autoselectDelay", autoselectDelay);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Can be called to manually notify a resizable and its descendant
     * resizables of a resize change.
     * </p>
     */
    protected void notifyResize() {
        getElement().callFunction("notifyResize");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Used to assign the closest resizable ancestor to this resizable if the
     * ancestor detects a request for notifications.
     * </p>
     * 
     * @param parentResizable
     *            Missing documentation!
     */
    protected void assignParentResizable(JsonObject parentResizable) {
        getElement().callFunction("assignParentResizable", parentResizable);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Used to remove a resizable descendant from the list of descendants that
     * should be notified of a resize change.
     * </p>
     * 
     * @param target
     *            Missing documentation!
     */
    protected void stopResizeNotificationsFor(JsonObject target) {
        getElement().callFunction("stopResizeNotificationsFor", target);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * This method can be overridden to filter nested elements that should or
     * should not be notified by the current element. Return true if an element
     * should be notified, or false if it should not be notified.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>boolean</code>. Functions with return types different than void are
     * not supported at this moment.
     * 
     * @param element
     *            A candidate descendant element that implements
     *            `IronResizableBehavior`.
     */
    @NotSupported
    protected void resizerShouldNotify(JsonObject element) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns the index of the given item.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>elemental.json.JsonObject</code>. Functions with return types
     * different than void are not supported at this moment.
     * 
     * @param item
     *            Missing documentation!
     */
    @NotSupported
    protected void indexOf(JsonObject item) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Selects the given value. If the {@code multi} property is true, then the
     * selected state of the {@code value} will be toggled; otherwise the
     * {@code value} will be selected.
     * </p>
     * 
     * @param value
     *            the value to select.
     */
    protected void select(String value) {
        getElement().callFunction("select", value);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Selects the given value. If the {@code multi} property is true, then the
     * selected state of the {@code value} will be toggled; otherwise the
     * {@code value} will be selected.
     * </p>
     * 
     * @param value
     *            the value to select.
     */
    protected void select(double value) {
        getElement().callFunction("select", value);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Selects the previous item.
     * </p>
     */
    protected void selectPrevious() {
        getElement().callFunction("selectPrevious");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Selects the next item.
     * </p>
     */
    protected void selectNext() {
        getElement().callFunction("selectNext");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Selects the item at the given index.
     * </p>
     * 
     * @param index
     *            Missing documentation!
     */
    protected void selectIndex(JsonObject index) {
        getElement().callFunction("selectIndex", index);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Force a synchronous update of the {@code items} property.
     * </p>
     * <p>
     * NOTE: Consider listening for the {@code iron-items-changed} event to
     * respond to updates to the set of selectable items after updates to the
     * DOM list and selection state have been made.
     * </p>
     * <p>
     * WARNING: If you are using this method, you should probably consider an
     * alternate approach. Synchronously querying for items is potentially slow
     * for many use cases. The {@code items} property will update asynchronously
     * on its own to reflect selectable items in the DOM.
     * </p>
     */
    protected void forceSynchronousItemUpdate() {
        getElement().callFunction("forceSynchronousItemUpdate");
    }

    /**
     * @param multi
     *            Missing documentation!
     */
    protected void multiChanged(JsonObject multi) {
        getElement().callFunction("multiChanged", multi);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Can be used to imperatively add a key binding to the implementing
     * element. This is the imperative equivalent of declaring a keybinding in
     * the {@code keyBindings} prototype property.
     * </p>
     * 
     * @param eventString
     *            Missing documentation!
     * @param handlerName
     *            Missing documentation!
     */
    protected void addOwnKeyBinding(String eventString, String handlerName) {
        getElement().callFunction("addOwnKeyBinding", eventString, handlerName);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When called, will remove all imperatively-added key bindings.
     * </p>
     */
    protected void removeOwnKeyBindings() {
        getElement().callFunction("removeOwnKeyBindings");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns true if a keyboard event matches {@code eventString}.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>boolean</code>. Functions with return types different than void are
     * not supported at this moment.
     * 
     * @param event
     *            Missing documentation!
     * @param eventString
     *            Missing documentation!
     */
    @NotSupported
    protected void keyboardEventMatchesKeys(JsonObject event,
            String eventString) {
    }

    @DomEvent("iron-activate")
    public static class IronActivateEvent<R extends GeneratedPaperTabs<R>>
            extends ComponentEvent<R> {
        public IronActivateEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code iron-activate} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addIronActivateListener(
            ComponentEventListener<IronActivateEvent<R>> listener) {
        return addListener(IronActivateEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("iron-deselect")
    public static class IronDeselectEvent<R extends GeneratedPaperTabs<R>>
            extends ComponentEvent<R> {
        public IronDeselectEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code iron-deselect} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addIronDeselectListener(
            ComponentEventListener<IronDeselectEvent<R>> listener) {
        return addListener(IronDeselectEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("iron-items-changed")
    public static class IronItemsChangeEvent<R extends GeneratedPaperTabs<R>>
            extends ComponentEvent<R> {
        public IronItemsChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code iron-items-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addIronItemsChangeListener(
            ComponentEventListener<IronItemsChangeEvent<R>> listener) {
        return addListener(IronItemsChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("iron-select")
    public static class IronSelectEvent<R extends GeneratedPaperTabs<R>>
            extends ComponentEvent<R> {
        public IronSelectEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code iron-select} events fired by the webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addIronSelectListener(
            ComponentEventListener<IronSelectEvent<R>> listener) {
        return addListener(IronSelectEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("selected-changed")
    public static class SelectedChangeEvent<R extends GeneratedPaperTabs<R>>
            extends ComponentEvent<R> {
        public SelectedChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code selected-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addSelectedChangeListener(
            ComponentEventListener<SelectedChangeEvent<R>> listener) {
        return addListener(SelectedChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("selected-item-changed")
    public static class SelectedItemChangeEvent<R extends GeneratedPaperTabs<R>>
            extends ComponentEvent<R> {
        public SelectedItemChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
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
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addSelectedItemChangeListener(
            ComponentEventListener<SelectedItemChangeEvent<R>> listener) {
        return addListener(SelectedItemChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("items-changed")
    public static class ItemsChangeEvent<R extends GeneratedPaperTabs<R>>
            extends ComponentEvent<R> {
        public ItemsChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code items-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addItemsChangeListener(
            ComponentEventListener<ItemsChangeEvent<R>> listener) {
        return addListener(ItemsChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("selected-values-changed")
    public static class SelectedValuesChangeEvent<R extends GeneratedPaperTabs<R>>
            extends ComponentEvent<R> {
        public SelectedValuesChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code selected-values-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addSelectedValuesChangeListener(
            ComponentEventListener<SelectedValuesChangeEvent<R>> listener) {
        return addListener(SelectedValuesChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("selected-items-changed")
    public static class SelectedItemsChangeEvent<R extends GeneratedPaperTabs<R>>
            extends ComponentEvent<R> {
        public SelectedItemsChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code selected-items-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addSelectedItemsChangeListener(
            ComponentEventListener<SelectedItemsChangeEvent<R>> listener) {
        return addListener(SelectedItemsChangeEvent.class,
                (ComponentEventListener) listener);
    }
}