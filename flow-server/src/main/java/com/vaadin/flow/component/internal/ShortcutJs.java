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
package com.vaadin.flow.component.internal;

import java.io.Serializable;

import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;

/**
 * The keydown delegate of a keyboard shortcut, as a JavaScript definition for
 * {@link Element#executeJs(Class)}. The function it calls is installed by the
 * shortcut helper that the client bundle carries.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @see ShortcutFilterJs
 */
@JsDefinition
public interface ShortcutJs extends Serializable {

    /**
     * Relays the keydown events of an element that only exists in the browser
     * to the element this definition was obtained from, the listen-on component
     * of a shortcut. A keydown that matches the given key and modifiers is
     * re-dispatched to the listen-on element, and its propagation is stopped.
     *
     * @param locateDelegate
     *            the function that returns the element to listen on, called
     *            with the listen-on element as <code>this</code>, see
     *            {@link Shortcuts#setShortcutListenOnElement(JsFunction, com.vaadin.flow.component.Component)}
     * @param requiredModifiers
     *            the names of the modifiers that have to be pressed, separated
     *            by spaces
     * @param forbiddenModifiers
     *            the names of the modifiers that must not be pressed, separated
     *            by spaces
     * @param checkBoundary
     *            whether to ignore a keydown from an open popover or modal
     *            inside the element to listen on
     * @param resetFocus
     *            whether to reset the focus of the active element before the
     *            keydown is relayed
     * @param allowDefault
     *            whether to allow the default behavior of a matching keydown
     * @param keys
     *            the key codes and key values that the keydown matches
     */
    @JsExpression("""
            window.Vaadin.Flow.shortcut.registerKeydownDelegate(this, $0, {
              keys: $6,
              requiredModifiers: $1,
              forbiddenModifiers: $2,
              checkBoundary: $3,
              resetFocus: $4,
              allowDefault: $5
            });""")
    void registerKeydownDelegate(JsFunction locateDelegate,
            String requiredModifiers, String forbiddenModifiers,
            boolean checkBoundary, boolean resetFocus, boolean allowDefault,
            String... keys);
}
