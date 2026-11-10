/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.component;

import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

/**
 * Collections of methods for configuring more complex Shortcut interactions.
 * <p>
 * Unlike the shortcut methods offered by {@link Focusable} and
 * {@link ClickNotifier}, these methods allow for configuring the {@code
 * lifecycleOwner} directly, making it possible to add the shortcut onto any
 * component. The {@code lifecycleOwner} denotes the component to which the
 * shortcut is bound to. If the lifecycle owner is not attached, visible, or
 * enabled, the shortcut won't work, and vice-versa.
 *
 * @author Vaadin Ltd.
 * @since 1.3
 *
 * @see Focusable#addFocusShortcut(Key, KeyModifier...) for adding a shortcut
 *      for focusing the component
 * @see ClickNotifier#addClickShortcut(Key, KeyModifier...) for adding a
 *      shortcut which performs the click-action
 */
public final class Shortcuts {
    static final String NULL = "Parameter '%s' must not be null!";
    static final String ELEMENT_LOCATOR_JS_KEY = "_element_locator_js_key";

    private Shortcuts() {
    }

    /**
     * Invoke a {@link Command} when the shortcut is invoked.
     * <p>
     * Registering a shortcut using this method will tie it to {@code
     * lifecycleOwner} and the shortcut is available in the global scope.
     * <p>
     * By default, the shortcut's listener is bound to {@link UI}. The listening
     * component can be changed by calling
     * {@link ShortcutRegistration#listenOn(Component...)}.
     *
     * @param lifecycleOwner
     *            the component that controls, when the shortcut is active. If
     *            the component is either invisible or detached, the shortcut
     *            won't work. Cannot be {@code null}
     * @param command
     *            code to execute when the shortcut is invoked. Cannot be {@code
     *         null}
     * @param key
     *            primary {@link Key} used to trigger the shortcut. Cannot be
     *            {@code null}
     * @param keyModifiers
     *            {@link KeyModifier KeyModifiers} which also need to be pressed
     *            for the shortcut to trigger
     * @return {@link ShortcutRegistration} for configuring and removing the
     *         shortcut
     */
    public static ShortcutRegistration addShortcutListener(
            Component lifecycleOwner, Command command, Key key,
            KeyModifier... keyModifiers) {
        if (lifecycleOwner == null) {
            throw new IllegalArgumentException(
                    String.format(NULL, "lifecycleOwner"));
        }
        if (command == null) {
            throw new IllegalArgumentException(String.format(NULL, "command"));
        }
        if (key == null) {
            throw new IllegalArgumentException(String.format(NULL, "key"));
        }
        return new ShortcutRegistration(lifecycleOwner,
                () -> new Component[] { lifecycleOwner.getUIOrThrow() },
                event -> command.execute(), key).withModifiers(keyModifiers);
    }

    /**
     * Invoke a {@link ShortcutEventListener} when the shortcut is invoked.
     * <p>
     * Registering a shortcut using this method will tie it to {@code
     * lifecycleOwner} and the shortcut is available in the global scope.
     * <p>
     * By default, the shortcut's listener is bound to {@link UI}. The listening
     * component can be changed by calling
     * {@link ShortcutRegistration#listenOn(Component...)}.
     *
     * @param lifecycleOwner
     *            the component that controls, when the shortcut is active. If
     *            the component is either invisible or detached, the shortcut
     *            won't work. Cannot be {@code null}
     * @param listener
     *            listener to execute when the shortcut is invoked. Receives a
     *            {@link ShortcutEvent}. Cannot be {@code null}
     * @param key
     *            primary {@link Key} used to trigger the shortcut. Cannot be
     *            {@code null}
     * @param keyModifiers
     *            {@link KeyModifier KeyModifiers} which also need to be pressed
     *            for the shortcut to trigger
     * @return {@link ShortcutRegistration} for configuring and removing the
     *         shortcut
     */
    public static ShortcutRegistration addShortcutListener(
            Component lifecycleOwner, ShortcutEventListener listener, Key key,
            KeyModifier... keyModifiers) {

        if (lifecycleOwner == null) {
            throw new IllegalArgumentException(
                    String.format(NULL, "lifecycleOwner"));
        }
        if (listener == null) {
            throw new IllegalArgumentException(String.format(NULL, "listener"));
        }
        if (key == null) {
            throw new IllegalArgumentException(String.format(NULL, "key"));
        }
        return new ShortcutRegistration(lifecycleOwner,
                () -> new Component[] { lifecycleOwner.getUIOrThrow() },
                listener, key).withModifiers(keyModifiers);
    }

    /**
     * Setup an element, that is only accessible on the browser (not server
     * side), to listen for shortcut events on and delegate to the given
     * component. The element will be located in the browser by evaluating the
     * given JS expression. This needs to be set for each listen-on component
     * instance.
     * <p>
     * <b>This should be only used by component developers</b>, when their
     * component is used as the {@code listenOn} component for shortcuts, and
     * their component does some magic on the browser which means that the
     * shortcut events are not coming through from the actual element. Thus when
     * an application developer calls e.g. <br>
     * {@code myButton.addClickShortcut(Key.ENTER).listenOn(dialog);} <br>
     * the framework will automatically make sure the events are passed from the
     * browser only element to the listenOn component (dialog in this case).
     * <p>
     * The expression is evaluated with the element of the listen-on component
     * as {@code this}. The browser compiles it from a string, so it does not
     * work with a Content Security Policy that does not allow
     * {@code unsafe-eval}. Use
     * {@link #setShortcutListenOnElement(JsFunction, Component)} with a
     * declared function instead.
     *
     * @param elementLocatorJs
     *            js execution string that references the desired element in DOM
     *            or {@code null} to any remove existing locator
     * @param listenOnComponent
     *            the component that is setup for listening shortcuts on
     *            {@link ShortcutRegistration#listenOn(Component...)}
     * @return a registration for removing the locator, does not affect active
     *         shortcuts or if the locator has changed from what was set for
     *         this registration
     * @since 6.0.2
     * @deprecated The browser compiles the locator from a string. Use
     *             {@link #setShortcutListenOnElement(JsFunction, Component)}
     *             with a function declared in a {@link JsDefinition} interface.
     */
    @Deprecated(since = "25.4", forRemoval = true)
    public static Registration setShortcutListenOnElement(
            String elementLocatorJs, Component listenOnComponent) {
        return setElementLocator(elementLocatorJs, listenOnComponent);
    }

    /**
     * Setup an element, that is only accessible on the browser (not server
     * side), to listen for shortcut events on and delegate to the given
     * component. The element will be located in the browser by calling the
     * given function. This needs to be set for each listen-on component
     * instance.
     * <p>
     * <b>This should be only used by component developers</b>, when their
     * component is used as the {@code listenOn} component for shortcuts, and
     * their component does some magic on the browser which means that the
     * shortcut events are not coming through from the actual element. Thus when
     * an application developer calls e.g. <br>
     * {@code myButton.addClickShortcut(Key.ENTER).listenOn(dialog);} <br>
     * the framework will automatically make sure the events are passed from the
     * browser only element to the listenOn component (dialog in this case).
     * <p>
     * The function is called without arguments and with the element of the
     * listen-on component as {@code this}, and returns the element to listen
     * on. Declare it in a {@link JsDefinition} interface, so that the browser
     * does not compile it from a string, which also works with a Content
     * Security Policy that does not allow {@code unsafe-eval}:
     *
     * <pre>
     * &#64;JsDefinition
     * public interface OverlayJs extends Serializable {
     *
     *     &#64;JsExpression("return this.$.overlay;")
     *     JsFunction locateOverlay();
     * }
     *
     * Shortcuts.setShortcutListenOnElement(
     *         JsSnippets.of(OverlayJs.class).locateOverlay(), this);
     * </pre>
     * <p>
     * Unlike {@link #setShortcutListenOnElement(String, Component)}, which
     * takes a JavaScript expression that the browser compiles, this takes a
     * function that the build adds to the bundle.
     *
     * @param elementLocator
     *            the function that returns the desired element in DOM, or
     *            {@code null} to remove any existing locator, passed as
     *            {@code (JsFunction) null} to tell the overloads apart
     * @param listenOnComponent
     *            the component that is setup for listening shortcuts on
     *            {@link ShortcutRegistration#listenOn(Component...)}
     * @return a registration for removing the locator, does not affect active
     *         shortcuts or if the locator has changed from what was set for
     *         this registration
     */
    public static Registration setShortcutListenOnElement(
            JsFunction elementLocator, Component listenOnComponent) {
        return setElementLocator(elementLocator, listenOnComponent);
    }

    private static Registration setElementLocator(Object elementLocator,
            Component listenOnComponent) {
        // a bit wasteful to store this for each component instance, but it
        // stays in memory only as long as the component itself does
        ComponentUtil.setData(listenOnComponent, ELEMENT_LOCATOR_JS_KEY,
                elementLocator);
        return () -> {
            if (elementLocator != null && elementLocator.equals(ComponentUtil
                    .getData(listenOnComponent, ELEMENT_LOCATOR_JS_KEY))) {
                ComponentUtil.setData(listenOnComponent, ELEMENT_LOCATOR_JS_KEY,
                        null);
            }
        };
    }

}
