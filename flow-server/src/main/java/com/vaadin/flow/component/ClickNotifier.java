/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.io.Serializable;

import com.vaadin.flow.shared.Registration;

/**
 * Mixin interface for components that support adding click listeners to the
 * their root elements.
 *
 * @param <T>
 *            the type of the component returned at the
 *            {@link ClickEvent#getSource()}
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface ClickNotifier<T extends Component> extends Serializable {
    /**
     * Adds a click listener to this component.
     *
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return a handle that can be used for removing the listener
     */
    default Registration addClickListener(
            ComponentEventListener<ClickEvent<T>> listener) {
        if (this instanceof Component) {
            return ComponentUtil.addListener((Component) this, ClickEvent.class,
                    (ComponentEventListener) listener);
        } else {
            throw new IllegalStateException(String.format(
                    "The class '%s' doesn't extend '%s'. "
                            + "Make your implementation for the method '%s'.",
                    getClass().getName(), Component.class.getSimpleName(),
                    "addClickListener"));
        }
    }

    /**
     * Adds a shortcut which 'clicks' the {@link Component} which implements
     * {@link ClickNotifier} interface. The shortcut's event listener is in
     * global scope and the shortcut's lifecycle is tied to {@code this}
     * component.
     * <p>
     * Use the returned {@link ShortcutRegistration} to fluently configure the
     * shortcut.
     * <p>
     * By default, the returned {@code ShortcutRegistration} allows
     * browser's default behavior, unlike other {@code ShortcutRegistrations}.
     * This is used to make sure that value synchronization of input fields is
     * not blocked for the shortcut key (e.g. Enter key).
     * To change this behavior, call
     * {@link ShortcutRegistration#setBrowserDefaultAllowed(boolean)}.
     *
     * @param key
     *              primary {@link Key} used to trigger the shortcut. Cannot
     *              be null.
     * @param keyModifiers
     *              {@link KeyModifier KeyModifiers} that need to be pressed
     *              along with the {@code key} for the shortcut to trigger
     * @return  {@link ShortcutRegistration} for configuring the shortcut and
     *          removing
     */
    default ShortcutRegistration addClickShortcut(Key key,
                                                  KeyModifier... keyModifiers) {
        if (!(this instanceof Component)) {
            throw new IllegalStateException(String.format(
                    "The class '%s' doesn't extend '%s'. "
                            + "Make your implementation for the method '%s'.",
                    getClass().getName(), Component.class.getSimpleName(),
                    "addClickShortcut(Key, KeyModifier...)"));
        }

        if (key == null) {
            throw new IllegalArgumentException(
                    String.format(Shortcuts.NULL, "key"));
        }

        final Component thisComponent = (Component) this;
        return new ShortcutRegistration(thisComponent, UI::getCurrent,
                event -> ComponentUtil.fireEvent(thisComponent,
                        new ClickEvent<>(thisComponent)),
                key).withModifiers(keyModifiers)
                .allowBrowserDefault();
    }
}
