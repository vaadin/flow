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
package com.vaadin.flow.component.trigger;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.trigger.internal.ConfigContext;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Fires when the given key (with optional modifiers) is pressed while the host
 * component is the active scope. The shortcut listens for {@code keydown}
 * events that originate inside the host's root element, matching
 * {@code event.key} against the configured {@link Key} and
 * {@code event.ctrlKey / altKey / shiftKey / metaKey} against the configured
 * {@link KeyModifier} set.
 * <p>
 * Bound actions run inside the keydown handler, preserving the user-gesture
 * context.
 *
 * <pre>{@code
 * new ShortcutTrigger(form, Key.ENTER, KeyModifier.CONTROL)
 *         .triggers(new ClickAction(submitButton));
 * }</pre>
 */
public class ShortcutTrigger extends AbstractTrigger {

    public static final String TYPE_ID = "flow:shortcut";

    private final Key key;
    private final Set<KeyModifier> modifiers;

    /**
     * Creates a shortcut trigger bound to the given host component.
     *
     * @param host
     *            the component acting as the shortcut's scope, not {@code null}
     * @param key
     *            the key to listen for, not {@code null}
     * @param modifiers
     *            modifier keys that must be held; empty for none
     */
    public ShortcutTrigger(Component host, Key key, KeyModifier... modifiers) {
        super(TYPE_ID, host);
        this.key = Objects.requireNonNull(key);
        this.modifiers = modifiers.length == 0
                ? EnumSet.noneOf(KeyModifier.class)
                : EnumSet.copyOf(java.util.Arrays.asList(modifiers));
    }

    /**
     * @return the key this shortcut listens for
     */
    public Key getKey() {
        return key;
    }

    /**
     * @return an unmodifiable view of the modifier set
     */
    public Set<KeyModifier> getModifiers() {
        return java.util.Collections.unmodifiableSet(modifiers);
    }

    @Override
    public ObjectNode buildClientConfig(ConfigContext context) {
        ObjectNode node = JacksonUtils.createObjectNode();
        // Key.getKeys() returns the list of event.key values that map to the
        // logical key. Send the first one; the others are browser/OS variants
        // and the canonical one is enough for matching in v0.
        node.put("key", key.getKeys().get(0));
        ArrayNode mods = JacksonUtils.createArrayNode();
        for (KeyModifier modifier : modifiers) {
            mods.add(modifier.getKeys().get(0));
        }
        node.set("modifiers", mods);
        return node;
    }
}
