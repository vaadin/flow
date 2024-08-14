package com.vaadin.flow.server.dau;

import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;

/**
 * A context for {@link UserIdentitySupplier} configurable function. Holds the
 * current instances of HTTP servlet request and Vaadin request and Vaadin
 * session.
 *
 * @param request
 *            current HTTP servlet request, may be {@literal null} if vaadin
 *            request is given.
 * @param request
 *            current Vaadin request, may be null if {@literal null} HTTP
 *            servlet request is given.
 * @param session
 *            current Vaadin session, can be {@literal null}
 *
 * @since 24.5
 */
public record UserIdentityContext(HttpServletRequest request,
        VaadinRequest vaadinRequest,
        VaadinSession session) implements Serializable {
}
