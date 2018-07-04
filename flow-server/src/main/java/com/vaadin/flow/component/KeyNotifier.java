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
 * Mixin interface for components that support adding key event listeners to the
 * their root elements.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface KeyNotifier extends Serializable {

    /**
     * Adds a {@code keydown} listener to this component.
     *
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return a handle that can be used for removing the listener
     */
    default Registration addKeyDownListener(
            ComponentEventListener<KeyDownEvent> listener) {
        if (this instanceof Component) {
            return ComponentUtil.addListener((Component) this,
                    KeyDownEvent.class, listener);
        } else {
            throw new IllegalStateException(String.format(
                    "The class '%s' doesn't extend '%s'. "
                            + "Make your implementation for the method '%s'.",
                    getClass().getName(), Component.class.getSimpleName(),
                    "addKeyDownListener"));
        }
    }

    /**
     * Adds a {@code keypress} listener to this component.
     *
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return a handle that can be used for removing the listener
     */
    default Registration addKeyPressListener(
            ComponentEventListener<KeyPressEvent> listener) {
        if (this instanceof Component) {
            return ComponentUtil.addListener((Component) this,
                    KeyPressEvent.class, listener);
        } else {
            throw new IllegalStateException(String.format(
                    "The class '%s' doesn't extend '%s'. "
                            + "Make your implementation for the method '%s'.",
                    getClass().getName(), Component.class.getSimpleName(),
                    "addKeyPressListener"));
        }
    }

    /**
     * Adds a {@code keyup} listener to this component.
     *
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return a handle that can be used for removing the listener
     */
    default Registration addKeyUpListener(
            ComponentEventListener<KeyUpEvent> listener) {
        if (this instanceof Component) {
            return ComponentUtil.addListener((Component) this, KeyUpEvent.class,
                    listener);
        } else {
            throw new IllegalStateException(String.format(
                    "The class '%s' doesn't extend '%s'. "
                            + "Make your implementation for the method '%s'.",
                    getClass().getName(), Component.class.getSimpleName(),
                    "addKeyUpListener"));
        }
    }

    /**
     * Adds a {@code keydown} listener to this component, which will trigger
     * only if the keys involved in the event match the {@code key} and
     * {@code modifiers} parameters.
     * <p>
     * See {@link Key} for common static instances or use
     * {@link Key#of(String, String...)} to get an instance from an arbitrary
     * value.
     *
     * @param key
     *            the key to match
     * @param listener
     *            the listener to add, not <code>null</code>
     * @param modifiers
     *            the optional modifiers to match
     * @return a handle that can be used for removing the listener
     */
    default Registration addKeyDownListener(Key key,
            ComponentEventListener<KeyDownEvent> listener,
            KeyModifier... modifiers) {
        return addKeyDownListener(
                new KeyEventListener<>(listener, key, modifiers));
    }

    /**
     * Adds a {@code keypress} listener to this component, which will trigger
     * only if the keys involved in the event match the {@code key} and
     * {@code modifiers} parameters.
     * <p>
     * See {@link Key} for common static instances or use
     * {@link Key#of(String, String...)} to get an instance from an arbitrary
     * value.
     *
     * @param key
     *            the key to match
     * @param listener
     *            the listener to add, not <code>null</code>
     * @param modifiers
     *            the optional modifiers to match
     * @return a handle that can be used for removing the listener
     */
    default Registration addKeyPressListener(Key key,
            ComponentEventListener<KeyPressEvent> listener,
            KeyModifier... modifiers) {
        return addKeyPressListener(
                new KeyEventListener<>(listener, key, modifiers));
    }

    /**
     * Adds a {@code keyup} listener to this component, which will trigger only
     * if the keys involved in the event match the {@code key} and
     * {@code modifiers} parameters.
     * <p>
     * See {@link Key} for common static instances or use
     * {@link Key#of(String, String...)} to get an instance from an arbitrary
     * value.
     *
     * @param key
     *            the key to match
     * @param listener
     *            the listener to add, not <code>null</code>
     * @param modifiers
     *            the optional modifiers to match
     * @return a handle that can be used for removing the listener
     */
    default Registration addKeyUpListener(Key key,
            ComponentEventListener<KeyUpEvent> listener,
            KeyModifier... modifiers) {
        return addKeyUpListener(
                new KeyEventListener<>(listener, key, modifiers));
    }

}
