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
package com.vaadin.ui.paper.iconbutton;

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

@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: paper-icon-button#2.0.1", "Flow#1.0-SNAPSHOT" })
@Tag("paper-icon-button")
@HtmlImport("frontend://bower_components/paper-icon-button/paper-icon-button.html")
public class GeneratedPaperIconButton<R extends GeneratedPaperIconButton<R>>
        extends Component implements HasStyle, ComponentSupplier<R> {

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
     * @return this instance, for method chaining
     */
    protected R setKeyEventTarget(JsonObject keyEventTarget) {
        getElement().setPropertyJson("keyEventTarget", keyEventTarget);
        return get();
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
     * @return this instance, for method chaining
     */
    public R setStopKeyboardEventPropagation(
            boolean stopKeyboardEventPropagation) {
        getElement().setProperty("stopKeyboardEventPropagation",
                stopKeyboardEventPropagation);
        return get();
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code keyBindings} property from the webcomponent
     */
    protected JsonObject protectedGetKeyBindings() {
        return (JsonObject) getElement().getPropertyRaw("keyBindings");
    }

    /**
     * @param keyBindings
     *            the JsonObject value to set
     * @return this instance, for method chaining
     */
    protected R setKeyBindings(JsonObject keyBindings) {
        getElement().setPropertyJson("keyBindings", keyBindings);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the user is currently holding down the button.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code pressed} property from the webcomponent
     */
    public boolean isPressed() {
        return getElement().getProperty("pressed", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the button toggles the active state with each tap or press of
     * the spacebar.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code toggles} property from the webcomponent
     */
    public boolean isToggles() {
        return getElement().getProperty("toggles", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the button toggles the active state with each tap or press of
     * the spacebar.
     * </p>
     * 
     * @param toggles
     *            the boolean value to set
     * @return this instance, for method chaining
     */
    public R setToggles(boolean toggles) {
        getElement().setProperty("toggles", toggles);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the button is a toggle and is currently in the active state.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'active-changed' event happens.
     * </p>
     * 
     * @return the {@code active} property from the webcomponent
     */
    @Synchronize(property = "active", value = "active-changed")
    public boolean isActive() {
        return getElement().getProperty("active", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the button is a toggle and is currently in the active state.
     * </p>
     * 
     * @param active
     *            the boolean value to set
     * @return this instance, for method chaining
     */
    public R setActive(boolean active) {
        getElement().setProperty("active", active);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the element is currently being pressed by a &quot;pointer,&quot;
     * which is loosely defined as mouse or touch input (but specifically
     * excluding keyboard input).
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code pointerDown} property from the webcomponent
     */
    public boolean isPointerDown() {
        return getElement().getProperty("pointerDown", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the input device that caused the element to receive focus was a
     * keyboard.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code receivedFocusFromKeyboard} property from the
     *         webcomponent
     */
    public boolean isReceivedFocusFromKeyboard() {
        return getElement().getProperty("receivedFocusFromKeyboard", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The aria attribute to be set if the button is a toggle and in the active
     * state.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code ariaActiveAttribute} property from the webcomponent
     */
    public String getAriaActiveAttribute() {
        return getElement().getProperty("ariaActiveAttribute");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The aria attribute to be set if the button is a toggle and in the active
     * state.
     * </p>
     * 
     * @param ariaActiveAttribute
     *            the String value to set
     * @return this instance, for method chaining
     */
    public R setAriaActiveAttribute(String ariaActiveAttribute) {
        getElement().setProperty("ariaActiveAttribute",
                ariaActiveAttribute == null ? "" : ariaActiveAttribute);
        return get();
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
     * @return this instance, for method chaining
     */
    public R setDisabled(boolean disabled) {
        getElement().setProperty("disabled", disabled);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the element will not produce a ripple effect when interacted
     * with via the pointer.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code noink} property from the webcomponent
     */
    public boolean isNoink() {
        return getElement().getProperty("noink", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the element will not produce a ripple effect when interacted
     * with via the pointer.
     * </p>
     * 
     * @param noink
     *            the boolean value to set
     * @return this instance, for method chaining
     */
    public R setNoink(boolean noink) {
        getElement().setProperty("noink", noink);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The URL of an image for the icon. If the src property is specified, the
     * icon property should not be.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code src} property from the webcomponent
     */
    public String getSrc() {
        return getElement().getProperty("src");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The URL of an image for the icon. If the src property is specified, the
     * icon property should not be.
     * </p>
     * 
     * @param src
     *            the String value to set
     * @return this instance, for method chaining
     */
    public R setSrc(String src) {
        getElement().setProperty("src", src == null ? "" : src);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Specifies the icon name or index in the set of icons available in the
     * icon's icon set. If the icon property is specified, the src property
     * should not be.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code icon} property from the webcomponent
     */
    public String getIcon() {
        return getElement().getProperty("icon");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Specifies the icon name or index in the set of icons available in the
     * icon's icon set. If the icon property is specified, the src property
     * should not be.
     * </p>
     * 
     * @param icon
     *            the String value to set
     * @return this instance, for method chaining
     */
    public R setIcon(String icon) {
        getElement().setProperty("icon", icon == null ? "" : icon);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Specifies the alternate text for the button, for accessibility.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code alt} property from the webcomponent
     */
    public String getAlt() {
        return getElement().getProperty("alt");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Specifies the alternate text for the button, for accessibility.
     * </p>
     * 
     * @param alt
     *            the String value to set
     * @return this instance, for method chaining
     */
    public R setAlt(String alt) {
        getElement().setProperty("alt", alt == null ? "" : alt);
        return get();
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
    public void addOwnKeyBinding(String eventString, String handlerName) {
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
    protected void keyboardEventMatchesKeys(JsonObject event,
            String eventString) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Ensures this element contains a ripple effect. For startup efficiency the
     * ripple effect is dynamically on demand when needed.
     * </p>
     * 
     * @param optTriggeringEvent
     *            (optional) event that triggered the ripple.
     */
    protected void ensureRipple(JsonObject optTriggeringEvent) {
        getElement().callFunction("ensureRipple", optTriggeringEvent);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns the {@code <paper-ripple>} element used by this element to create
     * ripple effects. The element's ripple is created on demand, when
     * necessary, and calling this method will force the ripple to be created.
     * </p>
     */
    public void getRipple() {
        getElement().callFunction("getRipple");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns true if this element currently contains a ripple effect.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>boolean</code>. Functions with return types different than void are
     * not supported at this moment.
     */
    @NotSupported
    protected void hasRipple() {
    }

    @DomEvent("active-changed")
    public static class ActiveChangeEvent<R extends GeneratedPaperIconButton<R>>
            extends ComponentEvent<R> {
        public ActiveChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code active-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addActiveChangeListener(
            ComponentEventListener<ActiveChangeEvent<R>> listener) {
        return addListener(ActiveChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("focused-changed")
    public static class FocusedChangeEvent<R extends GeneratedPaperIconButton<R>>
            extends ComponentEvent<R> {
        public FocusedChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code focused-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addFocusedChangeListener(
            ComponentEventListener<FocusedChangeEvent<R>> listener) {
        return addListener(FocusedChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("disabled-changed")
    public static class DisabledChangeEvent<R extends GeneratedPaperIconButton<R>>
            extends ComponentEvent<R> {
        public DisabledChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code disabled-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addDisabledChangeListener(
            ComponentEventListener<DisabledChangeEvent<R>> listener) {
        return addListener(DisabledChangeEvent.class,
                (ComponentEventListener) listener);
    }
}