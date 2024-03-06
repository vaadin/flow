/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import java.net.URI;

import com.vaadin.flow.router.internal.BeforeLeaveHandler;

/**
 * A listener that may be added to the {@link com.vaadin.flow.component.UI}
 * using
 * {@link com.vaadin.flow.component.UI#addBeforeLeaveListener(BeforeLeaveListener)}.
 * <p>
 * All listeners added this way will be informed when old components are
 * detached from the {@link com.vaadin.flow.component.UI}.
 * <p>
 * During this phase there is the possibility to reroute to another navigation
 * target or to postpone the navigation (to for instance get user input).
 * <p>
 * If a route target is left for reasons not under the control of the navigator
 * (for instance using
 * {@link com.vaadin.flow.component.page.Page#setLocation(URI)}, typing a URL
 * into the address bar, or closing the browser), listeners are not called.
 *
 * All BeforeLeaveListeners will be executed before the BeforeLeaveObservers. To
 * control the order of execution of BeforeLeaveListeners, see
 * {@link ListenerPriority}
 *
 * @since 1.0
 */
@FunctionalInterface
public interface BeforeLeaveListener extends BeforeLeaveHandler {
}
