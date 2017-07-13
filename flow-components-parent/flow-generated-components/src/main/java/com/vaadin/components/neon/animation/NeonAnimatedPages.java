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
package com.vaadin.components.neon.animation;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.components.neon.animation.NeonAnimatedPages;
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
 * Material design: [Meaningful
 * transitions](https://www.google.com/design/spec/animation
 * /meaningful-transitions.html)
 * 
 * {@code neon-animated-pages} manages a set of pages and runs an animation when
 * switching between them. Its children pages should implement
 * {@code Polymer.NeonAnimatableBehavior} and define {@code entry} and
 * {@code exit} animations to be run when switching to or switching out of the
 * page.
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.13-SNAPSHOT",
		"WebComponent: neon-animated-pages#2.0.1", "Flow#0.1.13-SNAPSHOT"})
@Tag("neon-animated-pages")
@HtmlImport("frontend://bower_components/neon-animation/neon-animated-pages.html")
public class NeonAnimatedPages extends Component
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
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends NeonAnimatedPages> R setAttrForSelected(
			java.lang.String attrForSelected) {
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
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends NeonAnimatedPages> R setSelected(java.lang.String selected) {
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
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public <R extends NeonAnimatedPages> R setSelected(double selected) {
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
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getActivateEvent() {
		return getElement().getProperty("activateEvent");
	}

	/**
	 * @param activateEvent
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends NeonAnimatedPages> R setActivateEvent(
			java.lang.String activateEvent) {
		getElement().setProperty("activateEvent",
				activateEvent == null ? "" : activateEvent);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This is a CSS selector string. If this is set, only items that match the
	 * CSS selector are selectable.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends NeonAnimatedPages> R setSelectable(
			java.lang.String selectable) {
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
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends NeonAnimatedPages> R setSelectedClass(
			java.lang.String selectedClass) {
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
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends NeonAnimatedPages> R setSelectedAttribute(
			java.lang.String selectedAttribute) {
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
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends NeonAnimatedPages> R setFallbackSelection(
			java.lang.String fallbackSelection) {
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
	 * Animation configuration. See README for more info.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getAnimationConfig() {
		return (JsonObject) getElement().getPropertyRaw("animationConfig");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Animation configuration. See README for more info.
	 * 
	 * @param animationConfig
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public <R extends NeonAnimatedPages> R setAnimationConfig(
			elemental.json.JsonObject animationConfig) {
		getElement().setPropertyJson("animationConfig", animationConfig);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Convenience property for setting an 'entry' animation. Do not set
	 * {@code animationConfig.entry} manually if using this. The animated node
	 * is set to {@code this} if using this property.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getEntryAnimation() {
		return getElement().getProperty("entryAnimation");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Convenience property for setting an 'entry' animation. Do not set
	 * {@code animationConfig.entry} manually if using this. The animated node
	 * is set to {@code this} if using this property.
	 * 
	 * @param entryAnimation
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends NeonAnimatedPages> R setEntryAnimation(
			java.lang.String entryAnimation) {
		getElement().setProperty("entryAnimation",
				entryAnimation == null ? "" : entryAnimation);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Convenience property for setting an 'exit' animation. Do not set
	 * {@code animationConfig.exit} manually if using this. The animated node is
	 * set to {@code this} if using this property.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getExitAnimation() {
		return getElement().getProperty("exitAnimation");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Convenience property for setting an 'exit' animation. Do not set
	 * {@code animationConfig.exit} manually if using this. The animated node is
	 * set to {@code this} if using this property.
	 * 
	 * @param exitAnimation
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends NeonAnimatedPages> R setExitAnimation(
			java.lang.String exitAnimation) {
		getElement().setProperty("exitAnimation",
				exitAnimation == null ? "" : exitAnimation);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * if true, the initial page selection will also be animated according to
	 * its animation config.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isAnimateInitialSelection() {
		return getElement().getProperty("animateInitialSelection", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * if true, the initial page selection will also be animated according to
	 * its animation config.
	 * 
	 * @param animateInitialSelection
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends NeonAnimatedPages> R setAnimateInitialSelection(
			boolean animateInitialSelection) {
		getElement().setProperty("animateInitialSelection",
				animateInitialSelection);
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
	 * Returns the index of the given item.
	 * 
	 * @param item
	 *            Missing documentation!
	 * @return It would return a interface elemental.json.JsonObject
	 */
	@NotSupported
	protected void indexOf(elemental.json.JsonObject item) {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Selects the given value.
	 * 
	 * @param value
	 *            the value to select.
	 * @param value
	 *            the value to select.
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
	 *            Missing documentation!
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
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An element implementing {@code Polymer.NeonAnimationRunnerBehavior} calls
	 * this method to configure an animation with an optional type. Elements
	 * implementing {@code Polymer.NeonAnimatableBehavior} should define the
	 * property {@code animationConfig}, which is either a configuration object
	 * or a map of animation type to array of configuration objects.
	 * 
	 * @param type
	 *            Missing documentation!
	 */
	public void getAnimationConfig(elemental.json.JsonObject type) {
		getElement().callFunction("getAnimationConfig", type);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Plays an animation with an optional {@code type}.
	 * 
	 * @param type
	 *            Missing documentation!
	 * @param cookie
	 *            Missing documentation!
	 */
	public void playAnimation(elemental.json.JsonObject type,
			elemental.json.JsonObject cookie) {
		getElement().callFunction("playAnimation", type, cookie);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Cancels the currently running animations.
	 */
	public void cancelAnimation() {
		getElement().callFunction("cancelAnimation");
	}

	@DomEvent("iron-activate")
	public static class IronActivateEvent
			extends
				ComponentEvent<NeonAnimatedPages> {
		public IronActivateEvent(NeonAnimatedPages source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronActivateListener(
			ComponentEventListener<IronActivateEvent> listener) {
		return addListener(IronActivateEvent.class, listener);
	}

	@DomEvent("iron-deselect")
	public static class IronDeselectEvent
			extends
				ComponentEvent<NeonAnimatedPages> {
		public IronDeselectEvent(NeonAnimatedPages source, boolean fromClient) {
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
				ComponentEvent<NeonAnimatedPages> {
		public IronItemsChangedEvent(NeonAnimatedPages source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronItemsChangedListener(
			ComponentEventListener<IronItemsChangedEvent> listener) {
		return addListener(IronItemsChangedEvent.class, listener);
	}

	@DomEvent("iron-select")
	public static class IronSelectEvent
			extends
				ComponentEvent<NeonAnimatedPages> {
		public IronSelectEvent(NeonAnimatedPages source, boolean fromClient) {
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
				ComponentEvent<NeonAnimatedPages> {
		public SelectedChangedEvent(NeonAnimatedPages source, boolean fromClient) {
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
				ComponentEvent<NeonAnimatedPages> {
		public SelectedItemChangedEvent(NeonAnimatedPages source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addSelectedItemChangedListener(
			ComponentEventListener<SelectedItemChangedEvent> listener) {
		return addListener(SelectedItemChangedEvent.class, listener);
	}

	@DomEvent("items-changed")
	public static class ItemsChangedEvent
			extends
				ComponentEvent<NeonAnimatedPages> {
		public ItemsChangedEvent(NeonAnimatedPages source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addItemsChangedListener(
			ComponentEventListener<ItemsChangedEvent> listener) {
		return addListener(ItemsChangedEvent.class, listener);
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected <R extends NeonAnimatedPages> R getSelf() {
		return (R) this;
	}

	/**
	 * Adds the given components as children of this component.
	 * 
	 * @param components
	 *            the components to add
	 * @see HasComponents#add(Component...)
	 */
	public NeonAnimatedPages(com.vaadin.ui.Component... components) {
		add(components);
	}

	/**
	 * Default constructor.
	 */
	public NeonAnimatedPages() {
	}
}