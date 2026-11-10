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

import com.vaadin.flow.dom.DomEventFilter;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;

/**
 * The filter of the keydown listener of a keyboard shortcut, as a JavaScript
 * definition for {@link JsSnippets#of(Class)}. The function it calls is
 * installed by the shortcut helper that the client bundle carries.
 * <p>
 * The keys, modifiers and options of a shortcut are captured values rather than
 * part of the JavaScript, so every shortcut runs the same function, and
 * shortcuts with the same settings share them through the constant pool.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @see ShortcutJs
 */
@JsDefinition
public interface ShortcutFilterJs extends Serializable {

    /**
     * Creates the filter for
     * {@link DomListenerRegistration#setFilterExpression(DomEventFilter)} that
     * passes a keydown that matches the given key and modifiers. A keydown that
     * passes gets the side effects that are asked for.
     *
     * @param requiredModifiers
     *            the names of the modifiers that have to be pressed, separated
     *            by spaces
     * @param forbiddenModifiers
     *            the names of the modifiers that must not be pressed, separated
     *            by spaces
     * @param checkOwnerScope
     *            whether to ignore a keydown that does not originate from the
     *            popover or modal scope of the shortcut owner
     * @param ownerSelector
     *            the selector that finds the owner element when the scope is
     *            checked, or <code>null</code> when the owner is the UI, which
     *            is never inside a popover or modal
     * @param preventDefault
     *            whether to prevent the default behavior of a matching keydown
     * @param stopPropagation
     *            whether to stop the propagation of a matching keydown
     * @param resetFocus
     *            whether to reset the focus of the active element for a
     *            matching keydown
     * @param keys
     *            the key codes and key values that the keydown matches
     * @return the filter, not <code>null</code>
     */
    @JsExpression("""
            return window.Vaadin.Flow.shortcut.matchKeydown(event, {
              keys: $7,
              requiredModifiers: $0,
              forbiddenModifiers: $1,
              checkOwnerScope: $2,
              ownerSelector: $3,
              preventDefault: $4,
              stopPropagation: $5,
              resetFocus: $6
            });""")
    DomEventFilter matchKeydown(String requiredModifiers,
            String forbiddenModifiers, boolean checkOwnerScope,
            String ownerSelector, boolean preventDefault,
            boolean stopPropagation, boolean resetFocus, String... keys);
}
