/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.hotswap.impl;

import jakarta.annotation.Priority;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.hotswap.VaadinHotswapper;
import com.vaadin.flow.server.VaadinSession;

import java.util.Set;

/**
 * Triggers UI refresh when hotswap occurs while an error view is displayed.
 * This ensures that fixing a broken class during development will refresh the
 * error page and attempt to re-navigate to the original location.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.0
 */
@Priority(100)
public class ErrorViewHotswapper implements VaadinHotswapper {

    @Override
    public boolean onClassLoadEvent(VaadinSession session,
            Set<Class<?>> classes, boolean redefined) {
        // Only process redefined classes (not first-time loads)
        if (!redefined) {
            return false;
        }

        // Check each UI in the session
        for (UI ui : session.getUIs()) {
            if (ui.isClosing()) {
                continue;
            }

            // If showing error view, trigger page reload
            if (ui.getInternals().isShowingErrorView()) {
                return true;
            }
        }
        return false;
    }
}
