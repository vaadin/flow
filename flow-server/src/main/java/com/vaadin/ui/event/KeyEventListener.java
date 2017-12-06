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
package com.vaadin.ui.event;

import java.util.Arrays;
import java.util.EnumSet;

public class KeyEventListener<E extends KeyboardEvent>
        implements ComponentEventListener<E> {

    private final ComponentEventListener<E> listener;

    private final String key;

    private final EnumSet<KeyModifier> modifiers;

    public KeyEventListener(ComponentEventListener<E> listener, String key,
            KeyModifier... modifiers) {
        this.listener = listener;
        this.key = key;
        if (modifiers.length > 0) {
            this.modifiers = EnumSet.of(modifiers[0],
                    Arrays.copyOfRange(modifiers, 1, modifiers.length));
        } else {
            this.modifiers = EnumSet.noneOf(KeyModifier.class);
        }
    }

    @Override
    public void onComponentEvent(E event) {
        if (modifiers.isEmpty()) {
            listener.onComponentEvent(event);
        } else {
            if (event.isCtrlKey() == modifiers.contains(KeyModifier.CONTROL)
                    && event.isShiftKey() == modifiers
                            .contains(KeyModifier.SHIFT)
                    && event.isAltKey() == modifiers.contains(KeyModifier.ALT)
                    && event.isMetaKey() == modifiers
                            .contains(KeyModifier.META)) {
                listener.onComponentEvent(event);
            }
        }
    }

}
