/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.dau;

import java.io.Serializable;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;

/**
 * A context for {@link UserIdentitySupplier} configurable function. Holds the
 * current instances of Vaadin request and Vaadin session.
 *
 * @param request
 *            current Vaadin request, never {@literal null}.
 * @param session
 *            current Vaadin session, can be {@literal null}
 *
 * @since 24.5
 */
public record UserIdentityContext(VaadinRequest request,
        VaadinSession session) implements Serializable {
}
