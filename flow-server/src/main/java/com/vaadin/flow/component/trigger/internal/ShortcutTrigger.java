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
package com.vaadin.flow.component.trigger.internal;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;

/**
 * Fires when a specific key + modifier combination is pressed on the host's
 * root element. Built on top of {@link KeyboardEventTrigger} with the typical
 * shortcut defaults pre-applied:
 *
 * <ul>
 * <li>listens for {@code keydown},
 * <li>filters by the configured {@link Key} (matching either {@code event.key}
 * or {@code event.code}),
 * <li>requires an exact match of the configured modifiers — modifiers passed to
 * the constructor must be pressed, all others must NOT be pressed (so
 * {@code Ctrl+S} doesn't fire on {@code Ctrl+Shift+S}, leaving that combo free
 * to bind separately),
 * <li>calls {@code preventDefault()} and {@code stopPropagation()} so the
 * browser doesn't act on the shortcut (e.g. open the Save dialog for
 * {@code Ctrl+S}) and ancestor shortcut handlers don't double-fire.
 * </ul>
 *
 * <p>
 * Example:
 *
 * <pre>{@code
 * new ShortcutTrigger(layout, Key.KEY_S, KeyModifier.CONTROL)
 *         .triggers(saveAction);
 * }</pre>
 *
 * <p>
 * Pass zero modifiers for a plain-key shortcut:
 * {@code new ShortcutTrigger(host, Key.ESCAPE)}.
 * <p>
 * {@link KeyModifier#ALT_GRAPH} is not supported — {@code KeyboardEvent} has no
 * {@code altGraphKey} flag, and supporting it via
 * {@code getModifierState("AltGraph")} is left for a follow-up.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class ShortcutTrigger extends KeyboardEventTrigger {

    private final EnumSet<KeyModifier> modifiers;

    /**
     * Creates a shortcut trigger that fires when {@code key} is pressed with
     * exactly the given {@code modifiers} held down.
     *
     * @param host
     *            the component whose root element listens for the shortcut, not
     *            {@code null}
     * @param key
     *            the key that completes the shortcut, not {@code null}
     * @param modifiers
     *            the modifiers that must be held; pass none for a plain-key
     *            shortcut. Must not contain {@link KeyModifier#ALT_GRAPH}.
     */
    public ShortcutTrigger(Component host, Key key, KeyModifier... modifiers) {
        super(host);
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(modifiers, "modifiers");
        this.modifiers = EnumSet.noneOf(KeyModifier.class);
        for (KeyModifier modifier : modifiers) {
            Objects.requireNonNull(modifier, "modifier");
            if (modifier == KeyModifier.ALT_GRAPH) {
                throw new IllegalArgumentException(
                        "ALT_GRAPH is not supported as a shortcut modifier");
            }
            this.modifiers.add(modifier);
        }
        forKeys(key);
        preventDefault();
        stopPropagation();
    }

    @Override
    protected void appendHandlerBody(StringBuilder body,
            List<Object> extraCaptures) {
        // Exact-match modifier guard: required modifiers must be pressed, all
        // others must NOT be pressed. Runs before the key filter so a
        // mismatching modifier set short-circuits before string comparison.
        body.append("if(");
        body.append(modifiers.contains(KeyModifier.CONTROL) ? "!e.ctrlKey"
                : "e.ctrlKey");
        body.append("||");
        body.append(modifiers.contains(KeyModifier.SHIFT) ? "!e.shiftKey"
                : "e.shiftKey");
        body.append("||");
        body.append(
                modifiers.contains(KeyModifier.ALT) ? "!e.altKey" : "e.altKey");
        body.append("||");
        body.append(modifiers.contains(KeyModifier.META) ? "!e.metaKey"
                : "e.metaKey");
        body.append(")return;");
        super.appendHandlerBody(body, extraCaptures);
    }
}
