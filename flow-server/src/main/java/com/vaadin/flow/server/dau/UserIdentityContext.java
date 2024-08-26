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
