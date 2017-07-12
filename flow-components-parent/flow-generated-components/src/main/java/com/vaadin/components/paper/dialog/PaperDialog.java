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
package com.vaadin.components.paper.dialog;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.components.paper.dialog.PaperDialog;
import com.vaadin.annotations.Synchronize;
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
 * Material design:
 * [Dialogs](https://www.google.com/design/spec/components/dialogs.html)
 * 
 * {@code <paper-dialog>} is a dialog with Material Design styling and optional
 * animations when it is opened or closed. It provides styles for a header,
 * content area, and an action area for buttons. You can use the
 * {@code <paper-dialog-scrollable>} element (in its own repository) if you need
 * a scrolling content area. To autofocus a specific child element after opening
 * the dialog, give it the {@code autofocus} attribute. See
 * {@code Polymer.PaperDialogBehavior} and {@code Polymer.IronOverlayBehavior}
 * for specifics.
 * 
 * For example, the following code implements a dialog with a header, scrolling
 * content area and buttons. Focus will be given to the {@code dialog-confirm}
 * button when the dialog is opened.
 * 
 * <paper-dialog> <h2>Header</h2> <paper-dialog-scrollable> Lorem ipsum...
 * </paper-dialog-scrollable> <div class="buttons"> <paper-button
 * dialog-dismiss>Cancel</paper-button> <paper-button dialog-confirm
 * autofocus>Accept</paper-button> </div> </paper-dialog>
 * 
 * ### Styling
 * 
 * See the docs for {@code Polymer.PaperDialogBehavior} for the custom
 * properties available for styling this element.
 * 
 * ### Animations
 * 
 * Set the {@code entry-animation} and/or {@code exit-animation} attributes to
 * add an animation when the dialog is opened or closed. See the documentation
 * in [PolymerElements/neon-animation](https://github.com/PolymerElements/neon-
 * animation) for more info.
 * 
 * For example:
 * 
 * <link rel="import"
 * href="components/neon-animation/animations/scale-up-animation.html"> <link
 * rel="import"
 * href="components/neon-animation/animations/fade-out-animation.html">
 * 
 * <paper-dialog entry-animation="scale-up-animation"
 * exit-animation="fade-out-animation"> <h2>Header</h2> <div>Dialog body</div>
 * </paper-dialog>
 * 
 * ### Accessibility
 * 
 * See the docs for {@code Polymer.PaperDialogBehavior} for accessibility
 * features implemented by this element.
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.13-SNAPSHOT",
		"WebComponent: paper-dialog#2.0.0", "Flow#0.1.13-SNAPSHOT"})
@Tag("paper-dialog")
@HtmlImport("frontend://bower_components/paper-dialog/paper-dialog.html")
public class PaperDialog extends Component implements HasStyle, HasComponents {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The element that will receive a {@code max-height}/{@code width}. By
	 * default it is the same as {@code this}, but it can be set to a child
	 * element. This is useful, for example, for implementing a scrolling region
	 * inside the element.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 *            The JsonObject value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setSizingTarget(
			elemental.json.JsonObject sizingTarget) {
		getElement().setPropertyJson("sizingTarget", sizingTarget);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The element to fit {@code this} into.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 *            The JsonObject value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setFitInto(
			elemental.json.JsonObject fitInto) {
		getElement().setPropertyJson("fitInto", fitInto);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Will position the element around the positionTarget without overlapping
	 * it.
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
	 * Will position the element around the positionTarget without overlapping
	 * it.
	 * 
	 * @param noOverlap
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setNoOverlap(boolean noOverlap) {
		getElement().setProperty("noOverlap", noOverlap);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The element that should be used to position the element. If not set, it
	 * will default to the parent node.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 *            The JsonObject value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setPositionTarget(
			elemental.json.JsonObject positionTarget) {
		getElement().setPropertyJson("positionTarget", positionTarget);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The orientation against which to align the element horizontally relative
	 * to the {@code positionTarget}. Possible values are "left", "right",
	 * "auto".
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
	 * The orientation against which to align the element horizontally relative
	 * to the {@code positionTarget}. Possible values are "left", "right",
	 * "auto".
	 * 
	 * @param horizontalAlign
	 *            The String value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setHorizontalAlign(
			java.lang.String horizontalAlign) {
		getElement().setProperty("horizontalAlign",
				horizontalAlign == null ? "" : horizontalAlign);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The orientation against which to align the element vertically relative to
	 * the {@code positionTarget}. Possible values are "top", "bottom", "auto".
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
	 * The orientation against which to align the element vertically relative to
	 * the {@code positionTarget}. Possible values are "top", "bottom", "auto".
	 * 
	 * @param verticalAlign
	 *            The String value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setVerticalAlign(
			java.lang.String verticalAlign) {
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
	 * If true, it will use {@code horizontalAlign} and {@code verticalAlign}
	 * values as preferred alignment and if there's not enough space, it will
	 * pick the values which minimize the cropping.
	 * 
	 * @param dynamicAlign
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setDynamicAlign(boolean dynamicAlign) {
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
	 *            The double value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setHorizontalOffset(double horizontalOffset) {
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
	 *            The double value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setVerticalOffset(double verticalOffset) {
		getElement().setProperty("verticalOffset", verticalOffset);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to auto-fit on attach.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setAutoFitOnAttach(boolean autoFitOnAttach) {
		getElement().setProperty("autoFitOnAttach", autoFitOnAttach);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the overlay is currently displayed.
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
	 * True if the overlay is currently displayed.
	 * 
	 * @param opened
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setOpened(boolean opened) {
		getElement().setProperty("opened", opened);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the overlay was canceled when it was last closed.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setCanceled(boolean canceled) {
		getElement().setProperty("canceled", canceled);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to display a backdrop behind the overlay. It traps the focus
	 * within the light DOM of the overlay.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setWithBackdrop(boolean withBackdrop) {
		getElement().setProperty("withBackdrop", withBackdrop);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable auto-focusing the overlay or child nodes with the
	 * {@code autofocus} attribute` when the overlay is opened.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setNoAutoFocus(boolean noAutoFocus) {
		getElement().setProperty("noAutoFocus", noAutoFocus);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable canceling the overlay with the ESC key.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setNoCancelOnEscKey(
			boolean noCancelOnEscKey) {
		getElement().setProperty("noCancelOnEscKey", noCancelOnEscKey);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable canceling the overlay by clicking outside it.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setNoCancelOnOutsideClick(
			boolean noCancelOnOutsideClick) {
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
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 *            The JsonObject value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setClosingReason(
			elemental.json.JsonObject closingReason) {
		getElement().setPropertyJson("closingReason", closingReason);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to enable restoring of focus when overlay is closed.
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
	 * Set to true to enable restoring of focus when overlay is closed.
	 * 
	 * @param restoreFocusOnClose
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setRestoreFocusOnClose(
			boolean restoreFocusOnClose) {
		getElement().setProperty("restoreFocusOnClose", restoreFocusOnClose);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to keep overlay always on top.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setAlwaysOnTop(boolean alwaysOnTop) {
		getElement().setProperty("alwaysOnTop", alwaysOnTop);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If {@code modal} is true, this implies {@code no-cancel-on-outside-click}
	 * , {@code no-cancel-on-esc-key} and {@code with-backdrop}.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isModal() {
		return getElement().getProperty("modal", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If {@code modal} is true, this implies {@code no-cancel-on-outside-click}
	 * , {@code no-cancel-on-esc-key} and {@code with-backdrop}.
	 * 
	 * @param modal
	 *            The boolean value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setModal(boolean modal) {
		getElement().setProperty("modal", modal);
		return getSelf();
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
	 *            The JsonObject value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setAnimationConfig(
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
	 *            The String value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setEntryAnimation(
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
	 *            The String value to set.
	 * @return this instance, for method chaining
	 */
	public <R extends PaperDialog> R setExitAnimation(
			java.lang.String exitAnimation) {
		getElement().setProperty("exitAnimation",
				exitAnimation == null ? "" : exitAnimation);
		return getSelf();
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
	 *            The original event
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

	@DomEvent("horizontal-offset-changed")
	public static class HorizontalOffsetChangedEvent
			extends
				ComponentEvent<PaperDialog> {
		public HorizontalOffsetChangedEvent(PaperDialog source,
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
				ComponentEvent<PaperDialog> {
		public VerticalOffsetChangedEvent(PaperDialog source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addVerticalOffsetChangedListener(
			ComponentEventListener<VerticalOffsetChangedEvent> listener) {
		return addListener(VerticalOffsetChangedEvent.class, listener);
	}

	@DomEvent("opened-changed")
	public static class OpenedChangedEvent extends ComponentEvent<PaperDialog> {
		public OpenedChangedEvent(PaperDialog source, boolean fromClient) {
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
				ComponentEvent<PaperDialog> {
		private final JsonObject event;

		public IronOverlayCanceledEvent(PaperDialog source, boolean fromClient,
				@EventData("event.event") elemental.json.JsonObject event) {
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
				ComponentEvent<PaperDialog> {
		private final JsonObject event;

		public IronOverlayClosedEvent(PaperDialog source, boolean fromClient,
				@EventData("event.event") elemental.json.JsonObject event) {
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
				ComponentEvent<PaperDialog> {
		public IronOverlayOpenedEvent(PaperDialog source, boolean fromClient) {
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
	protected <R extends PaperDialog> R getSelf() {
		return (R) this;
	}
}