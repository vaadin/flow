/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
