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
     * Adds a double click listener to this component.
     *
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return a handle that can be used for removing the listener
     */
    default Registration addDoubleClickListener(
            ComponentEventListener<ClickEvent<T>> listener) {
        if (this instanceof Component) {
            return ComponentUtil.addListener((Component) this, ClickEvent.class,
                    (ComponentEventListener) listener,
                    d -> d.setFilter("event.detail == 2"));
        } else {
            throw new IllegalStateException(String.format(
                    "The class '%s' doesn't extend '%s'. "
                            + "Make your implementation for the method '%s'.",
                    getClass().getName(), Component.class.getSimpleName(),
                    "addDoubleClickListener"));
        }
    }

    /**
     * Adds a single click listener to this component.
     *
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return a handle that can be used for removing the listener
     */
    default Registration addSingleClickListener(
            ComponentEventListener<ClickEvent<T>> listener) {
        if (this instanceof Component) {
            // If JavaScript click() function was used detail is < 1
            return ComponentUtil.addListener((Component) this, ClickEvent.class,
                    (ComponentEventListener) listener,
                    d -> d.setFilter("event.detail <= 1"));
        } else {
            throw new IllegalStateException(String.format(
                    "The class '%s' doesn't extend '%s'. "
                            + "Make your implementation for the method '%s'.",
                    getClass().getName(), Component.class.getSimpleName(),
                    "addSingleClickListener"));
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
     * By default, the returned {@code ShortcutRegistration} allows browser's
     * default behavior, unlike other {@code ShortcutRegistrations}. This is
     * used to make sure that value synchronization of input fields is not
     * blocked for the shortcut key (e.g. Enter key). To change this behavior,
     * call {@link ShortcutRegistration#setBrowserDefaultAllowed(boolean)}.
     *
     * @param key
     *            primary {@link Key} used to trigger the shortcut. Cannot be
     *            null.
     * @param keyModifiers
     *            {@link KeyModifier KeyModifiers} that need to be pressed along
     *            with the {@code key} for the shortcut to trigger
     * @return {@link ShortcutRegistration} for configuring the shortcut and
     *         removing
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

        return new ShortcutRegistration(thisComponent,
                () -> new Component[] { thisComponent.getUI().get() },
                event -> ComponentUtil.fireEvent(thisComponent,
                        new ClickEvent<>(thisComponent)),
                key).withModifiers(keyModifiers).allowBrowserDefault();
    }
}
