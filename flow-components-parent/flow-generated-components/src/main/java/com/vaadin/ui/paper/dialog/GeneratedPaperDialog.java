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
package com.vaadin.ui.paper.dialog;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.HasStyle;
import com.vaadin.ui.common.ComponentSupplier;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.ui.event.Synchronize;
import com.vaadin.ui.common.NotSupported;
import com.vaadin.ui.event.DomEvent;
import com.vaadin.ui.event.ComponentEvent;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.shared.Registration;
import com.vaadin.ui.common.HasComponents;

@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: paper-dialog#2.0.0", "Flow#1.0-SNAPSHOT" })
@Tag("paper-dialog")
@HtmlImport("frontend://bower_components/paper-dialog/paper-dialog.html")
public class GeneratedPaperDialog<R extends GeneratedPaperDialog<R>> extends
        Component implements HasStyle, ComponentSupplier<R>, HasComponents {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The element that will receive a {@code max-height}/{@code width}. By
     * default it is the same as {@code this}, but it can be set to a child
     * element. This is useful, for example, for implementing a scrolling region
     * inside the element.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code sizingTarget} property from the webcomponent
     */
    protected JsonObject protectedGetSizingTarget() {
        return (JsonObject) getElement().getPropertyRaw("sizingTarget");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The element that will receive a {@code max-height}/{@code width}. By
     * default it is the same as {@code this}, but it can be set to a child
     * element. This is useful, for example, for implementing a scrolling region
     * inside the element.
     * </p>
     * 
     * @param sizingTarget
     *            the JsonObject value to set
     */
    protected void setSizingTarget(elemental.json.JsonObject sizingTarget) {
        getElement().setPropertyJson("sizingTarget", sizingTarget);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The element to fit {@code this} into.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code fitInto} property from the webcomponent
     */
    protected JsonObject protectedGetFitInto() {
        return (JsonObject) getElement().getPropertyRaw("fitInto");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The element to fit {@code this} into.
     * </p>
     * 
     * @param fitInto
     *            the JsonObject value to set
     */
    protected void setFitInto(elemental.json.JsonObject fitInto) {
        getElement().setPropertyJson("fitInto", fitInto);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Will position the element around the positionTarget without overlapping
     * it.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code noOverlap} property from the webcomponent
     */
    public boolean isNoOverlap() {
        return getElement().getProperty("noOverlap", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Will position the element around the positionTarget without overlapping
     * it.
     * </p>
     * 
     * @param noOverlap
     *            the boolean value to set
     */
    public void setNoOverlap(boolean noOverlap) {
        getElement().setProperty("noOverlap", noOverlap);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The element that should be used to position the element. If not set, it
     * will default to the parent node.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code positionTarget} property from the webcomponent
     */
    protected JsonObject protectedGetPositionTarget() {
        return (JsonObject) getElement().getPropertyRaw("positionTarget");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The element that should be used to position the element. If not set, it
     * will default to the parent node.
     * </p>
     * 
     * @param positionTarget
     *            the JsonObject value to set
     */
    protected void setPositionTarget(elemental.json.JsonObject positionTarget) {
        getElement().setPropertyJson("positionTarget", positionTarget);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The orientation against which to align the element horizontally relative
     * to the {@code positionTarget}. Possible values are &quot;left&quot;,
     * &quot;right&quot;, &quot;center&quot;, &quot;auto&quot;.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code horizontalAlign} property from the webcomponent
     */
    public String getHorizontalAlign() {
        return getElement().getProperty("horizontalAlign");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The orientation against which to align the element horizontally relative
     * to the {@code positionTarget}. Possible values are &quot;left&quot;,
     * &quot;right&quot;, &quot;center&quot;, &quot;auto&quot;.
     * </p>
     * 
     * @param horizontalAlign
     *            the String value to set
     */
    public void setHorizontalAlign(String horizontalAlign) {
        getElement().setProperty("horizontalAlign",
                horizontalAlign == null ? "" : horizontalAlign);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The orientation against which to align the element vertically relative to
     * the {@code positionTarget}. Possible values are &quot;top&quot;,
     * &quot;bottom&quot;, &quot;middle&quot;, &quot;auto&quot;.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code verticalAlign} property from the webcomponent
     */
    public String getVerticalAlign() {
        return getElement().getProperty("verticalAlign");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The orientation against which to align the element vertically relative to
     * the {@code positionTarget}. Possible values are &quot;top&quot;,
     * &quot;bottom&quot;, &quot;middle&quot;, &quot;auto&quot;.
     * </p>
     * 
     * @param verticalAlign
     *            the String value to set
     */
    public void setVerticalAlign(String verticalAlign) {
        getElement().setProperty("verticalAlign",
                verticalAlign == null ? "" : verticalAlign);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, it will use {@code horizontalAlign} and {@code verticalAlign}
     * values as preferred alignment and if there's not enough space, it will
     * pick the values which minimize the cropping.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code dynamicAlign} property from the webcomponent
     */
    public boolean isDynamicAlign() {
        return getElement().getProperty("dynamicAlign", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, it will use {@code horizontalAlign} and {@code verticalAlign}
     * values as preferred alignment and if there's not enough space, it will
     * pick the values which minimize the cropping.
     * </p>
     * 
     * @param dynamicAlign
     *            the boolean value to set
     */
    public void setDynamicAlign(boolean dynamicAlign) {
        getElement().setProperty("dynamicAlign", dynamicAlign);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * A pixel value that will be added to the position calculated for the given
     * {@code horizontalAlign}, in the direction of alignment. You can think of
     * it as increasing or decreasing the distance to the side of the screen
     * given by {@code horizontalAlign}.
     * </p>
     * <p>
     * If {@code horizontalAlign} is &quot;left&quot; or &quot;center&quot;,
     * this offset will increase or decrease the distance to the left side of
     * the screen: a negative offset will move the dropdown to the left; a
     * positive one, to the right.
     * </p>
     * <p>
     * Conversely if {@code horizontalAlign} is &quot;right&quot;, this offset
     * will increase or decrease the distance to the right side of the screen: a
     * negative offset will move the dropdown to the right; a positive one, to
     * the left.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code horizontalOffset} property from the webcomponent
     */
    public double getHorizontalOffset() {
        return getElement().getProperty("horizontalOffset", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * A pixel value that will be added to the position calculated for the given
     * {@code horizontalAlign}, in the direction of alignment. You can think of
     * it as increasing or decreasing the distance to the side of the screen
     * given by {@code horizontalAlign}.
     * </p>
     * <p>
     * If {@code horizontalAlign} is &quot;left&quot; or &quot;center&quot;,
     * this offset will increase or decrease the distance to the left side of
     * the screen: a negative offset will move the dropdown to the left; a
     * positive one, to the right.
     * </p>
     * <p>
     * Conversely if {@code horizontalAlign} is &quot;right&quot;, this offset
     * will increase or decrease the distance to the right side of the screen: a
     * negative offset will move the dropdown to the right; a positive one, to
     * the left.
     * </p>
     * 
     * @param horizontalOffset
     *            the double value to set
     */
    public void setHorizontalOffset(double horizontalOffset) {
        getElement().setProperty("horizontalOffset", horizontalOffset);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * A pixel value that will be added to the position calculated for the given
     * {@code verticalAlign}, in the direction of alignment. You can think of it
     * as increasing or decreasing the distance to the side of the screen given
     * by {@code verticalAlign}.
     * </p>
     * <p>
     * If {@code verticalAlign} is &quot;top&quot; or &quot;middle&quot;, this
     * offset will increase or decrease the distance to the top side of the
     * screen: a negative offset will move the dropdown upwards; a positive one,
     * downwards.
     * </p>
     * <p>
     * Conversely if {@code verticalAlign} is &quot;bottom&quot;, this offset
     * will increase or decrease the distance to the bottom side of the screen:
     * a negative offset will move the dropdown downwards; a positive one,
     * upwards.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code verticalOffset} property from the webcomponent
     */
    public double getVerticalOffset() {
        return getElement().getProperty("verticalOffset", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * A pixel value that will be added to the position calculated for the given
     * {@code verticalAlign}, in the direction of alignment. You can think of it
     * as increasing or decreasing the distance to the side of the screen given
     * by {@code verticalAlign}.
     * </p>
     * <p>
     * If {@code verticalAlign} is &quot;top&quot; or &quot;middle&quot;, this
     * offset will increase or decrease the distance to the top side of the
     * screen: a negative offset will move the dropdown upwards; a positive one,
     * downwards.
     * </p>
     * <p>
     * Conversely if {@code verticalAlign} is &quot;bottom&quot;, this offset
     * will increase or decrease the distance to the bottom side of the screen:
     * a negative offset will move the dropdown downwards; a positive one,
     * upwards.
     * </p>
     * 
     * @param verticalOffset
     *            the double value to set
     */
    public void setVerticalOffset(double verticalOffset) {
        getElement().setProperty("verticalOffset", verticalOffset);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to auto-fit on attach.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code autoFitOnAttach} property from the webcomponent
     */
    public boolean isAutoFitOnAttach() {
        return getElement().getProperty("autoFitOnAttach", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to auto-fit on attach.
     * </p>
     * 
     * @param autoFitOnAttach
     *            the boolean value to set
     */
    public void setAutoFitOnAttach(boolean autoFitOnAttach) {
        getElement().setProperty("autoFitOnAttach", autoFitOnAttach);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the overlay is currently displayed.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'opened-changed' event happens.
     * </p>
     * 
     * @return the {@code opened} property from the webcomponent
     */
    @Synchronize(property = "opened", value = "opened-changed")
    public boolean isOpened() {
        return getElement().getProperty("opened", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the overlay is currently displayed.
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
     * True if the overlay was canceled when it was last closed.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code canceled} property from the webcomponent
     */
    public boolean isCanceled() {
        return getElement().getProperty("canceled", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to display a backdrop behind the overlay. It traps the focus
     * within the light DOM of the overlay.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code withBackdrop} property from the webcomponent
     */
    public boolean isWithBackdrop() {
        return getElement().getProperty("withBackdrop", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to display a backdrop behind the overlay. It traps the focus
     * within the light DOM of the overlay.
     * </p>
     * 
     * @param withBackdrop
     *            the boolean value to set
     */
    public void setWithBackdrop(boolean withBackdrop) {
        getElement().setProperty("withBackdrop", withBackdrop);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable auto-focusing the overlay or child nodes with the
     * {@code autofocus} attribute` when the overlay is opened.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code noAutoFocus} property from the webcomponent
     */
    public boolean isNoAutoFocus() {
        return getElement().getProperty("noAutoFocus", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable auto-focusing the overlay or child nodes with the
     * {@code autofocus} attribute` when the overlay is opened.
     * </p>
     * 
     * @param noAutoFocus
     *            the boolean value to set
     */
    public void setNoAutoFocus(boolean noAutoFocus) {
        getElement().setProperty("noAutoFocus", noAutoFocus);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable canceling the overlay with the ESC key.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code noCancelOnEscKey} property from the webcomponent
     */
    public boolean isNoCancelOnEscKey() {
        return getElement().getProperty("noCancelOnEscKey", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable canceling the overlay with the ESC key.
     * </p>
     * 
     * @param noCancelOnEscKey
     *            the boolean value to set
     */
    public void setNoCancelOnEscKey(boolean noCancelOnEscKey) {
        getElement().setProperty("noCancelOnEscKey", noCancelOnEscKey);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable canceling the overlay by clicking outside it.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code noCancelOnOutsideClick} property from the webcomponent
     */
    public boolean isNoCancelOnOutsideClick() {
        return getElement().getProperty("noCancelOnOutsideClick", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable canceling the overlay by clicking outside it.
     * </p>
     * 
     * @param noCancelOnOutsideClick
     *            the boolean value to set
     */
    public void setNoCancelOnOutsideClick(boolean noCancelOnOutsideClick) {
        getElement().setProperty("noCancelOnOutsideClick",
                noCancelOnOutsideClick);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Contains the reason(s) this overlay was last closed (see
     * {@code iron-overlay-closed}). {@code IronOverlayBehavior} provides the
     * {@code canceled} reason; implementers of the behavior can provide other
     * reasons in addition to {@code canceled}.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code closingReason} property from the webcomponent
     */
    protected JsonObject protectedGetClosingReason() {
        return (JsonObject) getElement().getPropertyRaw("closingReason");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Contains the reason(s) this overlay was last closed (see
     * {@code iron-overlay-closed}). {@code IronOverlayBehavior} provides the
     * {@code canceled} reason; implementers of the behavior can provide other
     * reasons in addition to {@code canceled}.
     * </p>
     * 
     * @param closingReason
     *            the JsonObject value to set
     */
    protected void setClosingReason(elemental.json.JsonObject closingReason) {
        getElement().setPropertyJson("closingReason", closingReason);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to enable restoring of focus when overlay is closed.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code restoreFocusOnClose} property from the webcomponent
     */
    public boolean isRestoreFocusOnClose() {
        return getElement().getProperty("restoreFocusOnClose", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to enable restoring of focus when overlay is closed.
     * </p>
     * 
     * @param restoreFocusOnClose
     *            the boolean value to set
     */
    public void setRestoreFocusOnClose(boolean restoreFocusOnClose) {
        getElement().setProperty("restoreFocusOnClose", restoreFocusOnClose);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to allow clicks to go through overlays. When the user clicks
     * outside this overlay, the click may close the overlay below.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code allowClickThrough} property from the webcomponent
     */
    public boolean isAllowClickThrough() {
        return getElement().getProperty("allowClickThrough", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to allow clicks to go through overlays. When the user clicks
     * outside this overlay, the click may close the overlay below.
     * </p>
     * 
     * @param allowClickThrough
     *            the boolean value to set
     */
    public void setAllowClickThrough(boolean allowClickThrough) {
        getElement().setProperty("allowClickThrough", allowClickThrough);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to keep overlay always on top.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code alwaysOnTop} property from the webcomponent
     */
    public boolean isAlwaysOnTop() {
        return getElement().getProperty("alwaysOnTop", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to keep overlay always on top.
     * </p>
     * 
     * @param alwaysOnTop
     *            the boolean value to set
     */
    public void setAlwaysOnTop(boolean alwaysOnTop) {
        getElement().setProperty("alwaysOnTop", alwaysOnTop);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Determines which action to perform when scroll outside an opened overlay
     * happens. Possible values: lock - blocks scrolling from happening, refit -
     * computes the new position on the overlay cancel - causes the overlay to
     * close
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code scrollAction} property from the webcomponent
     */
    public String getScrollAction() {
        return getElement().getProperty("scrollAction");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Determines which action to perform when scroll outside an opened overlay
     * happens. Possible values: lock - blocks scrolling from happening, refit -
     * computes the new position on the overlay cancel - causes the overlay to
     * close
     * </p>
     * 
     * @param scrollAction
     *            the String value to set
     */
    public void setScrollAction(String scrollAction) {
        getElement().setProperty("scrollAction",
                scrollAction == null ? "" : scrollAction);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If {@code modal} is true, this implies {@code no-cancel-on-outside-click}, {@code no-cancel-on-esc-key} and {@code with-backdrop}.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code modal} property from the webcomponent
     */
    public boolean isModal() {
        return getElement().getProperty("modal", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If {@code modal} is true, this implies {@code no-cancel-on-outside-click}, {@code no-cancel-on-esc-key} and {@code with-backdrop}.
     * </p>
     * 
     * @param modal
     *            the boolean value to set
     */
    public void setModal(boolean modal) {
        getElement().setProperty("modal", modal);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Animation configuration. See README for more info.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code animationConfig} property from the webcomponent
     */
    protected JsonObject protectedGetAnimationConfig() {
        return (JsonObject) getElement().getPropertyRaw("animationConfig");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Animation configuration. See README for more info.
     * </p>
     * 
     * @param animationConfig
     *            the JsonObject value to set
     */
    protected void setAnimationConfig(
            elemental.json.JsonObject animationConfig) {
        getElement().setPropertyJson("animationConfig", animationConfig);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Convenience property for setting an 'entry' animation. Do not set
     * {@code animationConfig.entry} manually if using this. The animated node
     * is set to {@code this} if using this property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code entryAnimation} property from the webcomponent
     */
    public String getEntryAnimation() {
        return getElement().getProperty("entryAnimation");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Convenience property for setting an 'entry' animation. Do not set
     * {@code animationConfig.entry} manually if using this. The animated node
     * is set to {@code this} if using this property.
     * </p>
     * 
     * @param entryAnimation
     *            the String value to set
     */
    public void setEntryAnimation(String entryAnimation) {
        getElement().setProperty("entryAnimation",
                entryAnimation == null ? "" : entryAnimation);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Convenience property for setting an 'exit' animation. Do not set
     * {@code animationConfig.exit} manually if using this. The animated node is
     * set to {@code this} if using this property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code exitAnimation} property from the webcomponent
     */
    public String getExitAnimation() {
        return getElement().getProperty("exitAnimation");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Convenience property for setting an 'exit' animation. Do not set
     * {@code animationConfig.exit} manually if using this. The animated node is
     * set to {@code this} if using this property.
     * </p>
     * 
     * @param exitAnimation
     *            the String value to set
     */
    public void setExitAnimation(String exitAnimation) {
        getElement().setProperty("exitAnimation",
                exitAnimation == null ? "" : exitAnimation);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Positions and fits the element into the {@code fitInto} element.
     * </p>
     */
    public void fit() {
        getElement().callFunction("fit");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Resets the target element's position and size constraints, and clear the
     * memoized data.
     * </p>
     */
    public void resetFit() {
        getElement().callFunction("resetFit");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Equivalent to calling {@code resetFit()} and {@code fit()}. Useful to
     * call this after the element or the {@code fitInto} element has been
     * resized, or if any of the positioning properties (e.g.
     * {@code horizontalAlign, verticalAlign}) is updated. It preserves the
     * scroll position of the sizingTarget.
     * </p>
     */
    public void refit() {
        getElement().callFunction("refit");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Positions the element according to {@code horizontalAlign, verticalAlign}
     * .
     * </p>
     */
    public void position() {
        getElement().callFunction("position");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Constrains the size of the element to {@code fitInto} by setting
     * {@code max-height} and/or {@code max-width}.
     * </p>
     */
    public void constrain() {
        getElement().callFunction("constrain");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Centers horizontally and vertically if not already positioned. This also
     * sets {@code position:fixed}.
     * </p>
     */
    public void center() {
        getElement().callFunction("center");
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
    public void notifyResize() {
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
    protected void assignParentResizable(
            elemental.json.JsonObject parentResizable) {
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
    protected void stopResizeNotificationsFor(
            elemental.json.JsonObject target) {
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
    protected void resizerShouldNotify(elemental.json.JsonObject element) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The backdrop element.
     * </p>
     */
    public void backdropElement() {
        getElement().callFunction("backdropElement");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Toggle the opened state of the overlay.
     * </p>
     */
    public void toggle() {
        getElement().callFunction("toggle");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Open the overlay.
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
     * Close the overlay.
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
     * Cancels the overlay.
     * </p>
     * 
     * @param event
     *            The original event
     */
    protected void cancel(elemental.json.JsonObject event) {
        getElement().callFunction("cancel", event);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Invalidates the cached tabbable nodes. To be called when any of the
     * focusable content changes (e.g. a button is disabled).
     * </p>
     */
    public void invalidateTabbables() {
        getElement().callFunction("invalidateTabbables");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * An element implementing {@code Polymer.NeonAnimationRunnerBehavior} calls
     * this method to configure an animation with an optional type. Elements
     * implementing {@code Polymer.NeonAnimatableBehavior} should define the
     * property {@code animationConfig}, which is either a configuration object
     * or a map of animation type to array of configuration objects.
     * </p>
     * 
     * @param type
     *            Missing documentation!
     */
    protected void getAnimationConfig(elemental.json.JsonObject type) {
        getElement().callFunction("getAnimationConfig", type);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Plays an animation with an optional {@code type}.
     * </p>
     * 
     * @param type
     *            Missing documentation!
     * @param cookie
     *            Missing documentation!
     */
    protected void playAnimation(elemental.json.JsonObject type,
            elemental.json.JsonObject cookie) {
        getElement().callFunction("playAnimation", type, cookie);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Cancels the currently running animations.
     * </p>
     */
    public void cancelAnimation() {
        getElement().callFunction("cancelAnimation");
    }

    @DomEvent("horizontal-offset-changed")
    public static class HorizontalOffsetChangeEvent<R extends GeneratedPaperDialog<R>>
            extends ComponentEvent<R> {
        public HorizontalOffsetChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code horizontal-offset-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     */
    public Registration addHorizontalOffsetChangeListener(
            ComponentEventListener<HorizontalOffsetChangeEvent<R>> listener) {
        return addListener(HorizontalOffsetChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("vertical-offset-changed")
    public static class VerticalOffsetChangeEvent<R extends GeneratedPaperDialog<R>>
            extends ComponentEvent<R> {
        public VerticalOffsetChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code vertical-offset-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     */
    public Registration addVerticalOffsetChangeListener(
            ComponentEventListener<VerticalOffsetChangeEvent<R>> listener) {
        return addListener(VerticalOffsetChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("opened-changed")
    public static class OpenedChangeEvent<R extends GeneratedPaperDialog<R>>
            extends ComponentEvent<R> {
        public OpenedChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code opened-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     */
    public Registration addOpenedChangeListener(
            ComponentEventListener<OpenedChangeEvent<R>> listener) {
        return addListener(OpenedChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("iron-overlay-canceled")
    public static class IronOverlayCanceledEvent<R extends GeneratedPaperDialog<R>>
            extends ComponentEvent<R> {
        public IronOverlayCanceledEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code iron-overlay-canceled} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     */
    public Registration addIronOverlayCanceledListener(
            ComponentEventListener<IronOverlayCanceledEvent<R>> listener) {
        return addListener(IronOverlayCanceledEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("iron-overlay-closed")
    public static class IronOverlayClosedEvent<R extends GeneratedPaperDialog<R>>
            extends ComponentEvent<R> {
        public IronOverlayClosedEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code iron-overlay-closed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     */
    public Registration addIronOverlayClosedListener(
            ComponentEventListener<IronOverlayClosedEvent<R>> listener) {
        return addListener(IronOverlayClosedEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("iron-overlay-opened")
    public static class IronOverlayOpenedEvent<R extends GeneratedPaperDialog<R>>
            extends ComponentEvent<R> {
        public IronOverlayOpenedEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code iron-overlay-opened} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     */
    public Registration addIronOverlayOpenedListener(
            ComponentEventListener<IronOverlayOpenedEvent<R>> listener) {
        return addListener(IronOverlayOpenedEvent.class,
                (ComponentEventListener) listener);
    }

    /**
     * Adds the given components as children of this component.
     * 
     * @param components
     *            the components to add
     * @see HasComponents#add(Component...)
     */
    public GeneratedPaperDialog(com.vaadin.ui.Component... components) {
        add(components);
    }

    /**
     * Default constructor.
     */
    public GeneratedPaperDialog() {
    }
}