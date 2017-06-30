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
package com.vaadin.components.iron.dropdown;

import com.vaadin.ui.Component;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Synchronize;
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
 * {@code <iron-dropdown>} is a generalized element that is useful when you have
 * hidden content ({@code dropdown-content}) that is revealed due to some change
 * in state that should cause it to do so.
 * 
 * Note that this is a low-level element intended to be used as part of other
 * composite elements that cause dropdowns to be revealed.
 * 
 * Examples of elements that might be implemented using an {@code iron-dropdown}
 * include comboboxes, menubuttons, selects. The list goes on.
 * 
 * The {@code <iron-dropdown>} element exposes attributes that allow the
 * position of the {@code dropdown-content} relative to the
 * {@code dropdown-trigger} to be configured.
 * 
 * <iron-dropdown horizontal-align="right" vertical-align="top"> <div
 * slot="dropdown-content">Hello!</div> </iron-dropdown>
 * 
 * In the above example, the {@code <div>} assigned to the
 * {@code dropdown-content} slot will be hidden until the dropdown element has
 * {@code opened} set to true, or when the {@code open} method is called on the
 * element.
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.12-SNAPSHOT",
		"WebComponent: iron-dropdown#2.0.0", "Flow#0.1.12-SNAPSHOT"})
@Tag("iron-dropdown")
@HtmlImport("frontend://bower_components/iron-dropdown/iron-dropdown.html")
public class IronDropdown<R extends IronDropdown<R>> extends Component {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the element currently has focus.
	 */
	@Synchronize(property = "focused", value = "focused-changed")
	public boolean isFocused() {
		return getElement().getProperty("focused", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the element currently has focus.
	 * 
	 * @param focused
	 * @return This instance, for method chaining.
	 */
	public R setFocused(boolean focused) {
		getElement().setProperty("focused", focused);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the user cannot interact with this element.
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
	 * @return This instance, for method chaining.
	 */
	public R setDisabled(boolean disabled) {
		getElement().setProperty("disabled", disabled);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The EventTarget that will be firing relevant KeyboardEvents. Set it to
	 * {@code null} to disable the listeners.
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
	 * @return This instance, for method chaining.
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
	 * @return This instance, for method chaining.
	 */
	public R setStopKeyboardEventPropagation(
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
	 * @return This instance, for method chaining.
	 */
	public R setKeyBindings(elemental.json.JsonObject keyBindings) {
		getElement().setPropertyJson("keyBindings", keyBindings);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The element that will receive a {@code max-height}/{@code width}. By
	 * default it is the same as {@code this}, but it can be set to a child
	 * element. This is useful, for example, for implementing a scrolling region
	 * inside the element.
	 */
	public JsonObject getSizingTarget() {
		return (JsonObject) getElement().getPropertyRaw("sizingTarget");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The element that will receive a {@code max-height}/{@code width}. By
	 * default it is the same as {@code this}, but it can be set to a child
	 * element. This is useful, for example, for implementing a scrolling region
	 * inside the element.
	 * 
	 * @param sizingTarget
	 * @return This instance, for method chaining.
	 */
	public R setSizingTarget(elemental.json.JsonObject sizingTarget) {
		getElement().setPropertyJson("sizingTarget", sizingTarget);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The element to fit {@code this} into.
	 */
	public JsonObject getFitInto() {
		return (JsonObject) getElement().getPropertyRaw("fitInto");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The element to fit {@code this} into.
	 * 
	 * @param fitInto
	 * @return This instance, for method chaining.
	 */
	public R setFitInto(elemental.json.JsonObject fitInto) {
		getElement().setPropertyJson("fitInto", fitInto);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Will position the element around the positionTarget without overlapping
	 * it.
	 */
	public boolean isNoOverlap() {
		return getElement().getProperty("noOverlap", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Will position the element around the positionTarget without overlapping
	 * it.
	 * 
	 * @param noOverlap
	 * @return This instance, for method chaining.
	 */
	public R setNoOverlap(boolean noOverlap) {
		getElement().setProperty("noOverlap", noOverlap);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The element that should be used to position the element. If not set, it
	 * will default to the parent node.
	 */
	public JsonObject getPositionTarget() {
		return (JsonObject) getElement().getPropertyRaw("positionTarget");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The element that should be used to position the element. If not set, it
	 * will default to the parent node.
	 * 
	 * @param positionTarget
	 * @return This instance, for method chaining.
	 */
	public R setPositionTarget(elemental.json.JsonObject positionTarget) {
		getElement().setPropertyJson("positionTarget", positionTarget);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The orientation against which to align the dropdown content horizontally
	 * relative to the dropdown trigger. Overridden from
	 * {@code Polymer.IronFitBehavior}.
	 */
	public String getHorizontalAlign() {
		return getElement().getProperty("horizontalAlign");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The orientation against which to align the dropdown content horizontally
	 * relative to the dropdown trigger. Overridden from
	 * {@code Polymer.IronFitBehavior}.
	 * 
	 * @param horizontalAlign
	 * @return This instance, for method chaining.
	 */
	public R setHorizontalAlign(java.lang.String horizontalAlign) {
		getElement().setProperty("horizontalAlign",
				horizontalAlign == null ? "" : horizontalAlign);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The orientation against which to align the dropdown content vertically
	 * relative to the dropdown trigger. Overridden from
	 * {@code Polymer.IronFitBehavior}.
	 */
	public String getVerticalAlign() {
		return getElement().getProperty("verticalAlign");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The orientation against which to align the dropdown content vertically
	 * relative to the dropdown trigger. Overridden from
	 * {@code Polymer.IronFitBehavior}.
	 * 
	 * @param verticalAlign
	 * @return This instance, for method chaining.
	 */
	public R setVerticalAlign(java.lang.String verticalAlign) {
		getElement().setProperty("verticalAlign",
				verticalAlign == null ? "" : verticalAlign);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, it will use {@code horizontalAlign} and {@code verticalAlign}
	 * values as preferred alignment and if there's not enough space, it will
	 * pick the values which minimize the cropping.
	 */
	public boolean isDynamicAlign() {
		return getElement().getProperty("dynamicAlign", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, it will use {@code horizontalAlign} and {@code verticalAlign}
	 * values as preferred alignment and if there's not enough space, it will
	 * pick the values which minimize the cropping.
	 * 
	 * @param dynamicAlign
	 * @return This instance, for method chaining.
	 */
	public R setDynamicAlign(boolean dynamicAlign) {
		getElement().setProperty("dynamicAlign", dynamicAlign);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A pixel value that will be added to the position calculated for the given
	 * {@code horizontalAlign}, in the direction of alignment. You can think of
	 * it as increasing or decreasing the distance to the side of the screen
	 * given by {@code horizontalAlign}.
	 * 
	 * If {@code horizontalAlign} is "left", this offset will increase or
	 * decrease the distance to the left side of the screen: a negative offset
	 * will move the dropdown to the left; a positive one, to the right.
	 * 
	 * Conversely if {@code horizontalAlign} is "right", this offset will
	 * increase or decrease the distance to the right side of the screen: a
	 * negative offset will move the dropdown to the right; a positive one, to
	 * the left.
	 */
	public double getHorizontalOffset() {
		return getElement().getProperty("horizontalOffset", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A pixel value that will be added to the position calculated for the given
	 * {@code horizontalAlign}, in the direction of alignment. You can think of
	 * it as increasing or decreasing the distance to the side of the screen
	 * given by {@code horizontalAlign}.
	 * 
	 * If {@code horizontalAlign} is "left", this offset will increase or
	 * decrease the distance to the left side of the screen: a negative offset
	 * will move the dropdown to the left; a positive one, to the right.
	 * 
	 * Conversely if {@code horizontalAlign} is "right", this offset will
	 * increase or decrease the distance to the right side of the screen: a
	 * negative offset will move the dropdown to the right; a positive one, to
	 * the left.
	 * 
	 * @param horizontalOffset
	 * @return This instance, for method chaining.
	 */
	public R setHorizontalOffset(double horizontalOffset) {
		getElement().setProperty("horizontalOffset", horizontalOffset);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A pixel value that will be added to the position calculated for the given
	 * {@code verticalAlign}, in the direction of alignment. You can think of it
	 * as increasing or decreasing the distance to the side of the screen given
	 * by {@code verticalAlign}.
	 * 
	 * If {@code verticalAlign} is "top", this offset will increase or decrease
	 * the distance to the top side of the screen: a negative offset will move
	 * the dropdown upwards; a positive one, downwards.
	 * 
	 * Conversely if {@code verticalAlign} is "bottom", this offset will
	 * increase or decrease the distance to the bottom side of the screen: a
	 * negative offset will move the dropdown downwards; a positive one,
	 * upwards.
	 */
	public double getVerticalOffset() {
		return getElement().getProperty("verticalOffset", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A pixel value that will be added to the position calculated for the given
	 * {@code verticalAlign}, in the direction of alignment. You can think of it
	 * as increasing or decreasing the distance to the side of the screen given
	 * by {@code verticalAlign}.
	 * 
	 * If {@code verticalAlign} is "top", this offset will increase or decrease
	 * the distance to the top side of the screen: a negative offset will move
	 * the dropdown upwards; a positive one, downwards.
	 * 
	 * Conversely if {@code verticalAlign} is "bottom", this offset will
	 * increase or decrease the distance to the bottom side of the screen: a
	 * negative offset will move the dropdown downwards; a positive one,
	 * upwards.
	 * 
	 * @param verticalOffset
	 * @return This instance, for method chaining.
	 */
	public R setVerticalOffset(double verticalOffset) {
		getElement().setProperty("verticalOffset", verticalOffset);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to auto-fit on attach.
	 */
	public boolean isAutoFitOnAttach() {
		return getElement().getProperty("autoFitOnAttach", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to auto-fit on attach.
	 * 
	 * @param autoFitOnAttach
	 * @return This instance, for method chaining.
	 */
	public R setAutoFitOnAttach(boolean autoFitOnAttach) {
		getElement().setProperty("autoFitOnAttach", autoFitOnAttach);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the overlay is currently displayed.
	 */
	@Synchronize(property = "opened", value = "opened-changed")
	public boolean isOpened() {
		return getElement().getProperty("opened", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the overlay is currently displayed.
	 * 
	 * @param opened
	 * @return This instance, for method chaining.
	 */
	public R setOpened(boolean opened) {
		getElement().setProperty("opened", opened);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the overlay was canceled when it was last closed.
	 */
	public boolean isCanceled() {
		return getElement().getProperty("canceled", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the overlay was canceled when it was last closed.
	 * 
	 * @param canceled
	 * @return This instance, for method chaining.
	 */
	public R setCanceled(boolean canceled) {
		getElement().setProperty("canceled", canceled);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to display a backdrop behind the overlay. It traps the focus
	 * within the light DOM of the overlay.
	 */
	public boolean isWithBackdrop() {
		return getElement().getProperty("withBackdrop", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to display a backdrop behind the overlay. It traps the focus
	 * within the light DOM of the overlay.
	 * 
	 * @param withBackdrop
	 * @return This instance, for method chaining.
	 */
	public R setWithBackdrop(boolean withBackdrop) {
		getElement().setProperty("withBackdrop", withBackdrop);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable auto-focusing the overlay or child nodes with the
	 * {@code autofocus} attribute` when the overlay is opened.
	 */
	public boolean isNoAutoFocus() {
		return getElement().getProperty("noAutoFocus", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable auto-focusing the overlay or child nodes with the
	 * {@code autofocus} attribute` when the overlay is opened.
	 * 
	 * @param noAutoFocus
	 * @return This instance, for method chaining.
	 */
	public R setNoAutoFocus(boolean noAutoFocus) {
		getElement().setProperty("noAutoFocus", noAutoFocus);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable canceling the overlay with the ESC key.
	 */
	public boolean isNoCancelOnEscKey() {
		return getElement().getProperty("noCancelOnEscKey", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable canceling the overlay with the ESC key.
	 * 
	 * @param noCancelOnEscKey
	 * @return This instance, for method chaining.
	 */
	public R setNoCancelOnEscKey(boolean noCancelOnEscKey) {
		getElement().setProperty("noCancelOnEscKey", noCancelOnEscKey);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable canceling the overlay by clicking outside it.
	 */
	public boolean isNoCancelOnOutsideClick() {
		return getElement().getProperty("noCancelOnOutsideClick", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable canceling the overlay by clicking outside it.
	 * 
	 * @param noCancelOnOutsideClick
	 * @return This instance, for method chaining.
	 */
	public R setNoCancelOnOutsideClick(boolean noCancelOnOutsideClick) {
		getElement().setProperty("noCancelOnOutsideClick",
				noCancelOnOutsideClick);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Contains the reason(s) this overlay was last closed (see
	 * {@code iron-overlay-closed}). {@code IronOverlayBehavior} provides the
	 * {@code canceled} reason; implementers of the behavior can provide other
	 * reasons in addition to {@code canceled}.
	 */
	public JsonObject getClosingReason() {
		return (JsonObject) getElement().getPropertyRaw("closingReason");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Contains the reason(s) this overlay was last closed (see
	 * {@code iron-overlay-closed}). {@code IronOverlayBehavior} provides the
	 * {@code canceled} reason; implementers of the behavior can provide other
	 * reasons in addition to {@code canceled}.
	 * 
	 * @param closingReason
	 * @return This instance, for method chaining.
	 */
	public R setClosingReason(elemental.json.JsonObject closingReason) {
		getElement().setPropertyJson("closingReason", closingReason);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to enable restoring of focus when overlay is closed.
	 */
	public boolean isRestoreFocusOnClose() {
		return getElement().getProperty("restoreFocusOnClose", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to enable restoring of focus when overlay is closed.
	 * 
	 * @param restoreFocusOnClose
	 * @return This instance, for method chaining.
	 */
	public R setRestoreFocusOnClose(boolean restoreFocusOnClose) {
		getElement().setProperty("restoreFocusOnClose", restoreFocusOnClose);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to keep overlay always on top.
	 */
	public boolean isAlwaysOnTop() {
		return getElement().getProperty("alwaysOnTop", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to keep overlay always on top.
	 * 
	 * @param alwaysOnTop
	 * @return This instance, for method chaining.
	 */
	public R setAlwaysOnTop(boolean alwaysOnTop) {
		getElement().setProperty("alwaysOnTop", alwaysOnTop);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Animation configuration. See README for more info.
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
	 * @return This instance, for method chaining.
	 */
	public R setAnimationConfig(elemental.json.JsonObject animationConfig) {
		getElement().setPropertyJson("animationConfig", animationConfig);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Convenience property for setting an 'entry' animation. Do not set
	 * {@code animationConfig.entry} manually if using this. The animated node
	 * is set to {@code this} if using this property.
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
	 * @return This instance, for method chaining.
	 */
	public R setEntryAnimation(java.lang.String entryAnimation) {
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
	 * @return This instance, for method chaining.
	 */
	public R setExitAnimation(java.lang.String exitAnimation) {
		getElement().setProperty("exitAnimation",
				exitAnimation == null ? "" : exitAnimation);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An animation config. If provided, this will be used to animate the
	 * opening of the dropdown. Pass an Array for multiple animations. See
	 * {@code neon-animation} documentation for more animation configuration
	 * details.
	 */
	public JsonObject getOpenAnimationConfig() {
		return (JsonObject) getElement().getPropertyRaw("openAnimationConfig");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An animation config. If provided, this will be used to animate the
	 * opening of the dropdown. Pass an Array for multiple animations. See
	 * {@code neon-animation} documentation for more animation configuration
	 * details.
	 * 
	 * @param openAnimationConfig
	 * @return This instance, for method chaining.
	 */
	public R setOpenAnimationConfig(
			elemental.json.JsonObject openAnimationConfig) {
		getElement()
				.setPropertyJson("openAnimationConfig", openAnimationConfig);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An animation config. If provided, this will be used to animate the
	 * closing of the dropdown. Pass an Array for multiple animations. See
	 * {@code neon-animation} documentation for more animation configuration
	 * details.
	 */
	public JsonObject getCloseAnimationConfig() {
		return (JsonObject) getElement().getPropertyRaw("closeAnimationConfig");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An animation config. If provided, this will be used to animate the
	 * closing of the dropdown. Pass an Array for multiple animations. See
	 * {@code neon-animation} documentation for more animation configuration
	 * details.
	 * 
	 * @param closeAnimationConfig
	 * @return This instance, for method chaining.
	 */
	public R setCloseAnimationConfig(
			elemental.json.JsonObject closeAnimationConfig) {
		getElement().setPropertyJson("closeAnimationConfig",
				closeAnimationConfig);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If provided, this will be the element that will be focused when the
	 * dropdown opens.
	 */
	public JsonObject getFocusTarget() {
		return (JsonObject) getElement().getPropertyRaw("focusTarget");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If provided, this will be the element that will be focused when the
	 * dropdown opens.
	 * 
	 * @param focusTarget
	 * @return This instance, for method chaining.
	 */
	public R setFocusTarget(elemental.json.JsonObject focusTarget) {
		getElement().setPropertyJson("focusTarget", focusTarget);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable animations when opening and closing the dropdown.
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
	 * @return This instance, for method chaining.
	 */
	public R setNoAnimations(boolean noAnimations) {
		getElement().setProperty("noAnimations", noAnimations);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * By default, the dropdown will constrain scrolling on the page to itself
	 * when opened. Set to true in order to prevent scroll from being
	 * constrained to the dropdown when it opens.
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
	 * @return This instance, for method chaining.
	 */
	public R setAllowOutsideScroll(boolean allowOutsideScroll) {
		getElement().setProperty("allowOutsideScroll", allowOutsideScroll);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The element that is contained by the dropdown, if any.
	 */
	public JsonObject getContainedElement() {
		return (JsonObject) getElement().getPropertyRaw("containedElement");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The element that is contained by the dropdown, if any.
	 * 
	 * @param containedElement
	 * @return This instance, for method chaining.
	 */
	public R setContainedElement(elemental.json.JsonObject containedElement) {
		getElement().setPropertyJson("containedElement", containedElement);
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

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Positions and fits the element into the {@code fitInto} element.
	 */
	public void fit() {
		getElement().callFunction("fit");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Resets the target element's position and size constraints, and clear the
	 * memoized data.
	 */
	public void resetFit() {
		getElement().callFunction("resetFit");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Equivalent to calling {@code resetFit()} and {@code fit()}. Useful to
	 * call this after the element or the {@code fitInto} element has been
	 * resized, or if any of the positioning properties (e.g.
	 * {@code horizontalAlign, verticalAlign}) is updated. It preserves the
	 * scroll position of the sizingTarget.
	 */
	public void refit() {
		getElement().callFunction("refit");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Positions the element according to {@code horizontalAlign, verticalAlign}
	 * .
	 */
	public void position() {
		getElement().callFunction("position");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Constrains the size of the element to {@code fitInto} by setting
	 * {@code max-height} and/or {@code max-width}.
	 */
	public void constrain() {
		getElement().callFunction("constrain");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Centers horizontally and vertically if not already positioned. This also
	 * sets {@code position:fixed}.
	 */
	public void center() {
		getElement().callFunction("center");
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
	 * The backdrop element.
	 */
	public void backdropElement() {
		getElement().callFunction("backdropElement");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Toggle the opened state of the overlay.
	 */
	public void toggle() {
		getElement().callFunction("toggle");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Open the overlay.
	 */
	public void open() {
		getElement().callFunction("open");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Close the overlay.
	 */
	public void close() {
		getElement().callFunction("close");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Cancels the overlay.
	 * 
	 * @param event
	 */
	public void cancel(elemental.json.JsonObject event) {
		getElement().callFunction("cancel", event);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Invalidates the cached tabbable nodes. To be called when any of the
	 * focusable content changes (e.g. a button is disabled).
	 */
	public void invalidateTabbables() {
		getElement().callFunction("invalidateTabbables");
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
	 * @param cookie
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

	@DomEvent("focused-changed")
	public static class FocusedChangedEvent
			extends
				ComponentEvent<IronDropdown> {
		public FocusedChangedEvent(IronDropdown source, boolean fromClient) {
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
				ComponentEvent<IronDropdown> {
		public DisabledChangedEvent(IronDropdown source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addDisabledChangedListener(
			ComponentEventListener<DisabledChangedEvent> listener) {
		return addListener(DisabledChangedEvent.class, listener);
	}

	@DomEvent("horizontal-offset-changed")
	public static class HorizontalOffsetChangedEvent
			extends
				ComponentEvent<IronDropdown> {
		public HorizontalOffsetChangedEvent(IronDropdown source,
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
				ComponentEvent<IronDropdown> {
		public VerticalOffsetChangedEvent(IronDropdown source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addVerticalOffsetChangedListener(
			ComponentEventListener<VerticalOffsetChangedEvent> listener) {
		return addListener(VerticalOffsetChangedEvent.class, listener);
	}

	@DomEvent("opened-changed")
	public static class OpenedChangedEvent extends ComponentEvent<IronDropdown> {
		public OpenedChangedEvent(IronDropdown source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addOpenedChangedListener(
			ComponentEventListener<OpenedChangedEvent> listener) {
		return addListener(OpenedChangedEvent.class, listener);
	}

	@DomEvent("iron-overlay-canceled")
	public static class IronOverlayCanceledEvent
			extends
				ComponentEvent<IronDropdown> {
		private final JsonObject event;

		public IronOverlayCanceledEvent(IronDropdown source,
				boolean fromClient, @EventData("event.event") JsonObject event) {
			super(source, fromClient);
			this.event = event;
		}

		public JsonObject getEvent() {
			return event;
		}
	}

	public Registration addIronOverlayCanceledListener(
			ComponentEventListener<IronOverlayCanceledEvent> listener) {
		return addListener(IronOverlayCanceledEvent.class, listener);
	}

	@DomEvent("iron-overlay-closed")
	public static class IronOverlayClosedEvent
			extends
				ComponentEvent<IronDropdown> {
		private final JsonObject event;

		public IronOverlayClosedEvent(IronDropdown source, boolean fromClient,
				@EventData("event.event") JsonObject event) {
			super(source, fromClient);
			this.event = event;
		}

		public JsonObject getEvent() {
			return event;
		}
	}

	public Registration addIronOverlayClosedListener(
			ComponentEventListener<IronOverlayClosedEvent> listener) {
		return addListener(IronOverlayClosedEvent.class, listener);
	}

	@DomEvent("iron-overlay-opened")
	public static class IronOverlayOpenedEvent
			extends
				ComponentEvent<IronDropdown> {
		public IronOverlayOpenedEvent(IronDropdown source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronOverlayOpenedListener(
			ComponentEventListener<IronOverlayOpenedEvent> listener) {
		return addListener(IronOverlayOpenedEvent.class, listener);
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