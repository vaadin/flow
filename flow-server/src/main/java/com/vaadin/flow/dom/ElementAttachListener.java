/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.dom;

import java.io.Serializable;

/**
 * Listener for element attach events. It is invoked when the element is
 * attached to the UI.
 *
 * @since 1.0
 */
@FunctionalInterface
public interface ElementAttachListener extends Serializable {
    /**
     * Invoked when an element is attached to the UI.
     *
     * @param event
     *            the attach event fired
     */
    void onAttach(ElementAttachEvent event);
}
