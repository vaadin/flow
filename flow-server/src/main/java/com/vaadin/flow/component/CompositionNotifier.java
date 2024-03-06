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
 * Mixin interface for components that support adding composition listeners to
 * the their root elements.
 *
 * See <a href=
 * "https://developer.mozilla.org/docs/Web/API/CompositionEvent">CompositionEvent</a>
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface CompositionNotifier extends Serializable {

    /**
     * Adds a {@code compositionstart} listener to this component.
     *
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return a handle that can be used for removing the listener
     */
    default Registration addCompositionStartListener(
            ComponentEventListener<CompositionStartEvent> listener) {
        return ComponentUtil.addListener((Component) this,
                CompositionStartEvent.class, listener);
    }

    /**
     * Adds a {@code compositionupdate} listener to this component.
     *
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return a handle that can be used for removing the listener
     */
    default Registration addCompositionUpdateListener(
            ComponentEventListener<CompositionUpdateEvent> listener) {
        return ComponentUtil.addListener((Component) this,
                CompositionUpdateEvent.class, listener);
    }

    /**
     * Adds a {@code compositionend} listener to this component.
     *
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return a handle that can be used for removing the listener
     */
    default Registration addCompositionEndListener(
            ComponentEventListener<CompositionEndEvent> listener) {
        return ComponentUtil.addListener((Component) this,
                CompositionEndEvent.class, listener);
    }

}
