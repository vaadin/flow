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
 * Any attached component implementing this interface will receive an event
 * before a new navigation state is entered.
 * <p>
 * During this event phase there is the possibility to reroute to another
 * navigation target.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface BeforeEnterObserver extends BeforeEnterHandler {
}
