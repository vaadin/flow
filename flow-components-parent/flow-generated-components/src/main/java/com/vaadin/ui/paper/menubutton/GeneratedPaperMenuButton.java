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
package com.vaadin.ui.paper.menubutton;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
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

@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: PaperMenuButton#2.0.0", "Flow#1.0-SNAPSHOT" })
@Tag("paper-menu-button")
@HtmlImport("frontend://bower_components/paper-menu-button/paper-menu-button.html")
public class GeneratedPaperMenuButton<R extends GeneratedPaperMenuButton<R>>
        extends Component implements ComponentSupplier<R>, HasStyle {

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
    protected JsonObject protectedGetKeyEventTarget() {
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
    protected void setKeyEventTarget(elemental.json.JsonObject keyEventTarget) {
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
    public boolean isStopKeyboardEventPropagation() {
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
    public void setStopKeyboardEventPropagation(
            boolean stopKeyboardEventPropagation) {
        getElement().setProperty("stopKeyboardEventPropagation",
                stopKeyboardEventPropagation);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * To be used to express what combination of keys will trigger the relative
     * callback. e.g. {@code keyBindings: 'esc': '_onEscPressed'}}
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code keyBindings} property from the webcomponent
     */
    protected JsonObject protectedGetKeyBindings() {
        return (JsonObject) getElement().getPropertyRaw("keyBindings");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * To be used to express what combination of keys will trigger the relative
     * callback. e.g. {@code keyBindings: 'esc': '_onEscPressed'}}
     * </p>
     * 
     * @param keyBindings
     *            the JsonObject value to set
     */
    protected void setKeyBindings(elemental.json.JsonObject keyBindings) {
        getElement().setPropertyJson("keyBindings", keyBindings);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the element currently has focus.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'focused-changed' event happens.
     * </p>
     * 
     * @return the {@code focused} property from the webcomponent
     */
    @Synchronize(property = "focused", value = "focused-changed")
    public boolean isFocused() {
        return getElement().getProperty("focused", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the user cannot interact with this element.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'disabled-changed' event happens.
     * </p>
     * 
     * @return the {@code disabled} property from the webcomponent
     */
    @Synchronize(property = "disabled", value = "disabled-changed")
    public boolean isDisabled() {
        return getElement().getProperty("disabled", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the user cannot interact with this element.
     * </p>
     * 
     * @param disabled
     *            the boolean value to set
     */
    public void setDisabled(boolean disabled) {
        getElement().setProperty("disabled", disabled);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the content is currently displayed.
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
     * True if the content is currently displayed.
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
     * The orientation against which to align the menu dropdown horizontally
     * relative to the dropdown trigger.
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
     * The orientation against which to align the menu dropdown horizontally
     * relative to the dropdown trigger.
     * </p>
     * 
     * @param horizontalAlign
     *            the String value to set
     */
    public void setHorizontalAlign(java.lang.String horizontalAlign) {
        getElement().setProperty("horizontalAlign",
                horizontalAlign == null ? "" : horizontalAlign);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The orientation against which to align the menu dropdown vertically
     * relative to the dropdown trigger.
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
     * The orientation against which to align the menu dropdown vertically
     * relative to the dropdown trigger.
     * </p>
     * 
     * @param verticalAlign
     *            the String value to set
     */
    public void setVerticalAlign(java.lang.String verticalAlign) {
        getElement().setProperty("verticalAlign",
                verticalAlign == null ? "" : verticalAlign);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the {@code horizontalAlign} and {@code verticalAlign} properties
     * will be considered preferences instead of strict requirements when
     * positioning the dropdown and may be changed if doing so reduces the area
     * of the dropdown falling outside of {@code fitInto}.
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
     * If true, the {@code horizontalAlign} and {@code verticalAlign} properties
     * will be considered preferences instead of strict requirements when
     * positioning the dropdown and may be changed if doing so reduces the area
     * of the dropdown falling outside of {@code fitInto}.
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
     * {@code horizontalAlign}. Use a negative value to offset to the left, or a
     * positive value to offset to the right.
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
     * {@code horizontalAlign}. Use a negative value to offset to the left, or a
     * positive value to offset to the right.
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
     * {@code verticalAlign}. Use a negative value to offset towards the top, or
     * a positive value to offset towards the bottom.
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
     * {@code verticalAlign}. Use a negative value to offset towards the top, or
     * a positive value to offset towards the bottom.
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
     * If true, the dropdown will be positioned so that it doesn't overlap the
     * button.
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
     * If true, the dropdown will be positioned so that it doesn't overlap the
     * button.
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
     * Set to true to disable animations when opening and closing the dropdown.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code noAnimations} property from the webcomponent
     */
    public boolean isNoAnimations() {
        return getElement().getProperty("noAnimations", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable animations when opening and closing the dropdown.
     * </p>
     * 
     * @param noAnimations
     *            the boolean value to set
     */
    public void setNoAnimations(boolean noAnimations) {
        getElement().setProperty("noAnimations", noAnimations);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable automatically closing the dropdown after a
     * selection has been made.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code ignoreSelect} property from the webcomponent
     */
    public boolean isIgnoreSelect() {
        return getElement().getProperty("ignoreSelect", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable automatically closing the dropdown after a
     * selection has been made.
     * </p>
     * 
     * @param ignoreSelect
     *            the boolean value to set
     */
    public void setIgnoreSelect(boolean ignoreSelect) {
        getElement().setProperty("ignoreSelect", ignoreSelect);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to enable automatically closing the dropdown after an item
     * has been activated, even if the selection did not change.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code closeOnActivate} property from the webcomponent
     */
    public boolean isCloseOnActivate() {
        return getElement().getProperty("closeOnActivate", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to enable automatically closing the dropdown after an item
     * has been activated, even if the selection did not change.
     * </p>
     * 
     * @param closeOnActivate
     *            the boolean value to set
     */
    public void setCloseOnActivate(boolean closeOnActivate) {
        getElement().setProperty("closeOnActivate", closeOnActivate);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * An animation config. If provided, this will be used to animate the
     * opening of the dropdown.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code openAnimationConfig} property from the webcomponent
     */
    protected JsonObject protectedGetOpenAnimationConfig() {
        return (JsonObject) getElement().getPropertyRaw("openAnimationConfig");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * An animation config. If provided, this will be used to animate the
     * opening of the dropdown.
     * </p>
     * 
     * @param openAnimationConfig
     *            the JsonObject value to set
     */
    protected void setOpenAnimationConfig(
            elemental.json.JsonObject openAnimationConfig) {
        getElement().setPropertyJson("openAnimationConfig",
                openAnimationConfig);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * An animation config. If provided, this will be used to animate the
     * closing of the dropdown.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code closeAnimationConfig} property from the webcomponent
     */
    protected JsonObject protectedGetCloseAnimationConfig() {
        return (JsonObject) getElement().getPropertyRaw("closeAnimationConfig");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * An animation config. If provided, this will be used to animate the
     * closing of the dropdown.
     * </p>
     * 
     * @param closeAnimationConfig
     *            the JsonObject value to set
     */
    protected void setCloseAnimationConfig(
            elemental.json.JsonObject closeAnimationConfig) {
        getElement().setPropertyJson("closeAnimationConfig",
                closeAnimationConfig);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * By default, the dropdown will constrain scrolling on the page to itself
     * when opened. Set to true in order to prevent scroll from being
     * constrained to the dropdown when it opens.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code allowOutsideScroll} property from the webcomponent
     */
    public boolean isAllowOutsideScroll() {
        return getElement().getProperty("allowOutsideScroll", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * By default, the dropdown will constrain scrolling on the page to itself
     * when opened. Set to true in order to prevent scroll from being
     * constrained to the dropdown when it opens.
     * </p>
     * 
     * @param allowOutsideScroll
     *            the boolean value to set
     */
    public void setAllowOutsideScroll(boolean allowOutsideScroll) {
        getElement().setProperty("allowOutsideScroll", allowOutsideScroll);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Whether focus should be restored to the button when the menu closes.
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
     * Whether focus should be restored to the button when the menu closes.
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
     * The content element that is contained by the menu button, if any.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code contentElement} property from the webcomponent
     */
    protected JsonObject protectedGetContentElement() {
        return (JsonObject) getElement().getPropertyRaw("contentElement");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The content element that is contained by the menu button, if any.
     * </p>
     * 
     * @param contentElement
     *            the JsonObject value to set
     */
    protected void setContentElement(elemental.json.JsonObject contentElement) {
        getElement().setPropertyJson("contentElement", contentElement);
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
    public void addOwnKeyBinding(java.lang.String eventString,
            java.lang.String handlerName) {
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
    public void removeOwnKeyBindings() {
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
    protected void keyboardEventMatchesKeys(elemental.json.JsonObject event,
            java.lang.String eventString) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Toggles the drowpdown content between opened and closed.
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
     * Make the dropdown content appear as an overlay positioned relative to the
     * dropdown trigger.
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
     * Hide the dropdown content.
     * </p>
     */
    public void close() {
        getElement().callFunction("close");
    }

    @DomEvent("focused-changed")
    public static class FocusedChangeEvent<R extends GeneratedPaperMenuButton<R>>
            extends ComponentEvent<R> {
        public FocusedChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    public Registration addFocusedChangeListener(
            ComponentEventListener<FocusedChangeEvent<R>> listener) {
        return addListener(FocusedChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("disabled-changed")
    public static class DisabledChangeEvent<R extends GeneratedPaperMenuButton<R>>
            extends ComponentEvent<R> {
        public DisabledChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    public Registration addDisabledChangeListener(
            ComponentEventListener<DisabledChangeEvent<R>> listener) {
        return addListener(DisabledChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("paper-dropdown-close")
    public static class PaperDropdownCloseEvent<R extends GeneratedPaperMenuButton<R>>
            extends ComponentEvent<R> {
        public PaperDropdownCloseEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    public Registration addPaperDropdownCloseListener(
            ComponentEventListener<PaperDropdownCloseEvent<R>> listener) {
        return addListener(PaperDropdownCloseEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("paper-dropdown-open")
    public static class PaperDropdownOpenEvent<R extends GeneratedPaperMenuButton<R>>
            extends ComponentEvent<R> {
        public PaperDropdownOpenEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    public Registration addPaperDropdownOpenListener(
            ComponentEventListener<PaperDropdownOpenEvent<R>> listener) {
        return addListener(PaperDropdownOpenEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("opened-changed")
    public static class OpenedChangeEvent<R extends GeneratedPaperMenuButton<R>>
            extends ComponentEvent<R> {
        public OpenedChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    public Registration addOpenedChangeListener(
            ComponentEventListener<OpenedChangeEvent<R>> listener) {
        return addListener(OpenedChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("horizontal-offset-changed")
    public static class HorizontalOffsetChangeEvent<R extends GeneratedPaperMenuButton<R>>
            extends ComponentEvent<R> {
        public HorizontalOffsetChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    public Registration addHorizontalOffsetChangeListener(
            ComponentEventListener<HorizontalOffsetChangeEvent<R>> listener) {
        return addListener(HorizontalOffsetChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("vertical-offset-changed")
    public static class VerticalOffsetChangeEvent<R extends GeneratedPaperMenuButton<R>>
            extends ComponentEvent<R> {
        public VerticalOffsetChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    public Registration addVerticalOffsetChangeListener(
            ComponentEventListener<VerticalOffsetChangeEvent<R>> listener) {
        return addListener(VerticalOffsetChangeEvent.class,
                (ComponentEventListener) listener);
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
     * @return this instance, for method chaining
     */
    public R addToDropdownTrigger(com.vaadin.ui.Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "dropdown-trigger");
            getElement().appendChild(component.getElement());
        }
        return get();
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
     * @return this instance, for method chaining
     */
    public R addToDropdownContent(com.vaadin.ui.Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "dropdown-content");
            getElement().appendChild(component.getElement());
        }
        return get();
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
        getElement().getChildren()
                .forEach(child -> child.removeAttribute("slot"));
        getElement().removeAllChildren();
    }
}