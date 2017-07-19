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
package com.vaadin.generated.paper.menu.button;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentSupplier;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.annotations.Synchronize;
import com.vaadin.components.NotSupported;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * Material design: [Dropdown
 * buttons](https://www.google.com/design/spec/components
 * /buttons.html#buttons-dropdown-buttons)
 * 
 * {@code paper-menu-button} allows one to compose a designated "trigger"
 * element with another element that represents "content", to create a dropdown
 * menu that displays the "content" when the "trigger" is clicked.
 * 
 * The child element assigned to the {@code dropdown-trigger} slot will be used
 * as the "trigger" element. The child element assigned to the
 * {@code dropdown-content} slot will be used as the "content" element.
 * 
 * The {@code paper-menu-button} is sensitive to its content's
 * {@code iron-select} events. If the "content" element triggers an
 * {@code iron-select} event, the {@code paper-menu-button} will close
 * automatically.
 * 
 * Example:
 * 
 * <paper-menu-button> <paper-icon-button icon="menu"
 * slot="dropdown-trigger"></paper-icon-button> <paper-listbox
 * slot="dropdown-content"> <paper-item>Share</paper-item>
 * <paper-item>Settings</paper-item> <paper-item>Help</paper-item>
 * </paper-listbox> </paper-menu-button>
 * 
 * ### Styling
 * 
 * The following custom properties and mixins are also available for styling:
 * 
 * Custom property | Description | Default
 * ----------------|-------------|----------
 * {@code --paper-menu-button-dropdown-background} | Background color of the
 * paper-menu-button dropdown | {@code --primary-background-color}
 * {@code --paper-menu-button} | Mixin applied to the paper-menu-button |
 * {@code {@code --paper-menu-button-disabled} | Mixin applied to the
 * paper-menu-button when disabled | {@code
 * {@code --paper-menu-button-dropdown} | Mixin applied to the paper-menu-button
 * dropdown | {@code {@code --paper-menu-button-content} | Mixin applied to the
 * paper-menu-button content | {@code
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.14-SNAPSHOT",
		"WebComponent: PaperMenuButton#2.0.0", "Flow#0.1.14-SNAPSHOT"})
@Tag("paper-menu-button")
@HtmlImport("frontend://bower_components/paper-menu-button/paper-menu-button.html")
public class GeneratedPaperMenuButton<R extends GeneratedPaperMenuButton<R>>
		extends
			Component implements ComponentSupplier<R>, HasStyle {

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
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public R setKeyEventTarget(elemental.json.JsonObject keyEventTarget) {
		getElement().setPropertyJson("keyEventTarget", keyEventTarget);
		return get();
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
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setStopKeyboardEventPropagation(
			boolean stopKeyboardEventPropagation) {
		getElement().setProperty("stopKeyboardEventPropagation",
				stopKeyboardEventPropagation);
		return get();
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
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public R setKeyBindings(elemental.json.JsonObject keyBindings) {
		getElement().setPropertyJson("keyBindings", keyBindings);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the element currently has focus.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'focused-changed' event happens.
	 */
	@Synchronize(property = "focused", value = "focused-changed")
	public boolean isFocused() {
		return getElement().getProperty("focused", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the user cannot interact with this element.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'disabled-changed' event happens.
	 */
	@Synchronize(property = "disabled", value = "disabled-changed")
	public boolean isDisabled() {
		return getElement().getProperty("disabled", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the user cannot interact with this element.
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
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the content is currently displayed.
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
	 * True if the content is currently displayed.
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
	 * The orientation against which to align the menu dropdown horizontally
	 * relative to the dropdown trigger.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getHorizontalAlign() {
		return getElement().getProperty("horizontalAlign");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The orientation against which to align the menu dropdown horizontally
	 * relative to the dropdown trigger.
	 * 
	 * @param horizontalAlign
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setHorizontalAlign(java.lang.String horizontalAlign) {
		getElement().setProperty("horizontalAlign",
				horizontalAlign == null ? "" : horizontalAlign);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The orientation against which to align the menu dropdown vertically
	 * relative to the dropdown trigger.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getVerticalAlign() {
		return getElement().getProperty("verticalAlign");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The orientation against which to align the menu dropdown vertically
	 * relative to the dropdown trigger.
	 * 
	 * @param verticalAlign
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setVerticalAlign(java.lang.String verticalAlign) {
		getElement().setProperty("verticalAlign",
				verticalAlign == null ? "" : verticalAlign);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the {@code horizontalAlign} and {@code verticalAlign} properties
	 * will be considered preferences instead of strict requirements when
	 * positioning the dropdown and may be changed if doing so reduces the area
	 * of the dropdown falling outside of {@code fitInto}.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isDynamicAlign() {
		return getElement().getProperty("dynamicAlign", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the {@code horizontalAlign} and {@code verticalAlign} properties
	 * will be considered preferences instead of strict requirements when
	 * positioning the dropdown and may be changed if doing so reduces the area
	 * of the dropdown falling outside of {@code fitInto}.
	 * 
	 * @param dynamicAlign
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setDynamicAlign(boolean dynamicAlign) {
		getElement().setProperty("dynamicAlign", dynamicAlign);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A pixel value that will be added to the position calculated for the given
	 * {@code horizontalAlign}. Use a negative value to offset to the left, or a
	 * positive value to offset to the right.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getHorizontalOffset() {
		return getElement().getProperty("horizontalOffset", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A pixel value that will be added to the position calculated for the given
	 * {@code horizontalAlign}. Use a negative value to offset to the left, or a
	 * positive value to offset to the right.
	 * 
	 * @param horizontalOffset
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public R setHorizontalOffset(double horizontalOffset) {
		getElement().setProperty("horizontalOffset", horizontalOffset);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A pixel value that will be added to the position calculated for the given
	 * {@code verticalAlign}. Use a negative value to offset towards the top, or
	 * a positive value to offset towards the bottom.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getVerticalOffset() {
		return getElement().getProperty("verticalOffset", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A pixel value that will be added to the position calculated for the given
	 * {@code verticalAlign}. Use a negative value to offset towards the top, or
	 * a positive value to offset towards the bottom.
	 * 
	 * @param verticalOffset
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public R setVerticalOffset(double verticalOffset) {
		getElement().setProperty("verticalOffset", verticalOffset);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the dropdown will be positioned so that it doesn't overlap the
	 * button.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isNoOverlap() {
		return getElement().getProperty("noOverlap", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the dropdown will be positioned so that it doesn't overlap the
	 * button.
	 * 
	 * @param noOverlap
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setNoOverlap(boolean noOverlap) {
		getElement().setProperty("noOverlap", noOverlap);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable animations when opening and closing the dropdown.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isNoAnimations() {
		return getElement().getProperty("noAnimations", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable animations when opening and closing the dropdown.
	 * 
	 * @param noAnimations
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setNoAnimations(boolean noAnimations) {
		getElement().setProperty("noAnimations", noAnimations);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable automatically closing the dropdown after a
	 * selection has been made.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isIgnoreSelect() {
		return getElement().getProperty("ignoreSelect", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable automatically closing the dropdown after a
	 * selection has been made.
	 * 
	 * @param ignoreSelect
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setIgnoreSelect(boolean ignoreSelect) {
		getElement().setProperty("ignoreSelect", ignoreSelect);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to enable automatically closing the dropdown after an item
	 * has been activated, even if the selection did not change.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isCloseOnActivate() {
		return getElement().getProperty("closeOnActivate", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to enable automatically closing the dropdown after an item
	 * has been activated, even if the selection did not change.
	 * 
	 * @param closeOnActivate
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setCloseOnActivate(boolean closeOnActivate) {
		getElement().setProperty("closeOnActivate", closeOnActivate);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An animation config. If provided, this will be used to animate the
	 * opening of the dropdown.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getOpenAnimationConfig() {
		return (JsonObject) getElement().getPropertyRaw("openAnimationConfig");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An animation config. If provided, this will be used to animate the
	 * opening of the dropdown.
	 * 
	 * @param openAnimationConfig
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public R setOpenAnimationConfig(
			elemental.json.JsonObject openAnimationConfig) {
		getElement()
				.setPropertyJson("openAnimationConfig", openAnimationConfig);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An animation config. If provided, this will be used to animate the
	 * closing of the dropdown.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getCloseAnimationConfig() {
		return (JsonObject) getElement().getPropertyRaw("closeAnimationConfig");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An animation config. If provided, this will be used to animate the
	 * closing of the dropdown.
	 * 
	 * @param closeAnimationConfig
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public R setCloseAnimationConfig(
			elemental.json.JsonObject closeAnimationConfig) {
		getElement().setPropertyJson("closeAnimationConfig",
				closeAnimationConfig);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * By default, the dropdown will constrain scrolling on the page to itself
	 * when opened. Set to true in order to prevent scroll from being
	 * constrained to the dropdown when it opens.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isAllowOutsideScroll() {
		return getElement().getProperty("allowOutsideScroll", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * By default, the dropdown will constrain scrolling on the page to itself
	 * when opened. Set to true in order to prevent scroll from being
	 * constrained to the dropdown when it opens.
	 * 
	 * @param allowOutsideScroll
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setAllowOutsideScroll(boolean allowOutsideScroll) {
		getElement().setProperty("allowOutsideScroll", allowOutsideScroll);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Whether focus should be restored to the button when the menu closes.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isRestoreFocusOnClose() {
		return getElement().getProperty("restoreFocusOnClose", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Whether focus should be restored to the button when the menu closes.
	 * 
	 * @param restoreFocusOnClose
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setRestoreFocusOnClose(boolean restoreFocusOnClose) {
		getElement().setProperty("restoreFocusOnClose", restoreFocusOnClose);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The content element that is contained by the menu button, if any.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getContentElement() {
		return (JsonObject) getElement().getPropertyRaw("contentElement");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The content element that is contained by the menu button, if any.
	 * 
	 * @param contentElement
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public R setContentElement(elemental.json.JsonObject contentElement) {
		getElement().setPropertyJson("contentElement", contentElement);
		return get();
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
	 * Toggles the drowpdown content between opened and closed.
	 */
	public void toggle() {
		getElement().callFunction("toggle");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Make the dropdown content appear as an overlay positioned relative to the
	 * dropdown trigger.
	 */
	public void open() {
		getElement().callFunction("open");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Hide the dropdown content.
	 */
	public void close() {
		getElement().callFunction("close");
	}

	@DomEvent("focused-changed")
	public static class FocusedChangedEvent
			extends
				ComponentEvent<GeneratedPaperMenuButton> {
		public FocusedChangedEvent(GeneratedPaperMenuButton source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addFocusedChangedListener(
			ComponentEventListener<FocusedChangedEvent> listener) {
		return addListener(FocusedChangedEvent.class, listener);
	}

	@DomEvent("disabled-changed")
	public static class DisabledChangedEvent
			extends
				ComponentEvent<GeneratedPaperMenuButton> {
		public DisabledChangedEvent(GeneratedPaperMenuButton source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addDisabledChangedListener(
			ComponentEventListener<DisabledChangedEvent> listener) {
		return addListener(DisabledChangedEvent.class, listener);
	}

	@DomEvent("paper-dropdown-close")
	public static class PaperDropdownCloseEvent
			extends
				ComponentEvent<GeneratedPaperMenuButton> {
		public PaperDropdownCloseEvent(GeneratedPaperMenuButton source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addPaperDropdownCloseListener(
			ComponentEventListener<PaperDropdownCloseEvent> listener) {
		return addListener(PaperDropdownCloseEvent.class, listener);
	}

	@DomEvent("paper-dropdown-open")
	public static class PaperDropdownOpenEvent
			extends
				ComponentEvent<GeneratedPaperMenuButton> {
		public PaperDropdownOpenEvent(GeneratedPaperMenuButton source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addPaperDropdownOpenListener(
			ComponentEventListener<PaperDropdownOpenEvent> listener) {
		return addListener(PaperDropdownOpenEvent.class, listener);
	}

	@DomEvent("opened-changed")
	public static class OpenedChangedEvent
			extends
				ComponentEvent<GeneratedPaperMenuButton> {
		public OpenedChangedEvent(GeneratedPaperMenuButton source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addOpenedChangedListener(
			ComponentEventListener<OpenedChangedEvent> listener) {
		return addListener(OpenedChangedEvent.class, listener);
	}

	@DomEvent("horizontal-offset-changed")
	public static class HorizontalOffsetChangedEvent
			extends
				ComponentEvent<GeneratedPaperMenuButton> {
		public HorizontalOffsetChangedEvent(GeneratedPaperMenuButton source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addHorizontalOffsetChangedListener(
			ComponentEventListener<HorizontalOffsetChangedEvent> listener) {
		return addListener(HorizontalOffsetChangedEvent.class, listener);
	}

	@DomEvent("vertical-offset-changed")
	public static class VerticalOffsetChangedEvent
			extends
				ComponentEvent<GeneratedPaperMenuButton> {
		public VerticalOffsetChangedEvent(GeneratedPaperMenuButton source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addVerticalOffsetChangedListener(
			ComponentEventListener<VerticalOffsetChangedEvent> listener) {
		return addListener(VerticalOffsetChangedEvent.class, listener);
	}

	/**
	 * Adds the given components as children of this component at the slot
	 * 'dropdown-trigger'.
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
	public void addToDropdownTrigger(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			component.getElement().setAttribute("slot", "dropdown-trigger");
			getElement().appendChild(component.getElement());
		}
	}

	/**
	 * Adds the given components as children of this component at the slot
	 * 'dropdown-content'.
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
	public void addToDropdownContent(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			component.getElement().setAttribute("slot", "dropdown-content");
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