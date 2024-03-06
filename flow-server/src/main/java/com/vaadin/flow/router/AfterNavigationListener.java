/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import com.vaadin.flow.router.internal.AfterNavigationHandler;

/**
 * A listener that may be added to the {@link com.vaadin.flow.component.UI}
 * using
 * {@link com.vaadin.flow.component.UI#addAfterNavigationListener(AfterNavigationListener)}.
 * <p>
 * All listeners added this way will be informed when new components have been
 * attached to the {@link com.vaadin.flow.component.UI} and all navigation tasks
 * have resolved.
 *
 * All AfterNavigationListeners will be executed before the
 * AfterNavigationObservers. To control the order of execution of
 * AfterNavigationListeners, see {@link ListenerPriority}
 *
 * @since 1.0
 */
@FunctionalInterface
public interface AfterNavigationListener extends AfterNavigationHandler {
}
