/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import com.vaadin.flow.router.internal.BeforeEnterHandler;

/**
 * A listener that may be added to the {@link com.vaadin.flow.component.UI}
 * using
 * {@link com.vaadin.flow.component.UI#addBeforeEnterListener(BeforeEnterListener)}.
 * <p>
 * All listeners added this way will be informed when a new set of components
 * are going to be attached to the {@link com.vaadin.flow.component.UI}.
 * <p>
 * During this phase there is the possibility to reroute to another navigation
 * target.
 *
 * All BeforeEnterListeners will be executed before the BeforeEnterObservers. To
 * control the order of execution of BeforeEnterListeners, see
 * {@link ListenerPriority}
 *
 * @since 1.0
 */
@FunctionalInterface
public interface BeforeEnterListener extends BeforeEnterHandler {
}
