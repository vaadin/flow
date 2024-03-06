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
import java.util.EventListener;

/**
 * Listener for shortcut events.
 *
 * @author Vaadin Ltd.
 * @since 1.3
 */
@FunctionalInterface
public interface ShortcutEventListener extends EventListener, Serializable {
    /**
     * Invoked when shortcut has been used.
     *
     * @param event
     *            {@link ShortcutEvent} based on the registered shortcut
     */
    void onShortcut(ShortcutEvent event);
}
