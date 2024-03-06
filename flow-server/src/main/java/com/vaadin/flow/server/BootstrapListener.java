/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.EventListener;

import com.vaadin.flow.server.communication.IndexHtmlRequestListener;

/**
 * This event listener is notified when the bootstrap HTML is about to be
 * generated and sent to the client. The bootstrap HTML is first constructed as
 * an in-memory DOM representation which registered listeners can modify before
 * the final HTML is generated.
 * <p>
 * BootstrapListeners are registered using the {@link ServiceInitEvent} during
 * the initialization of the application.
 *
 * @see ServiceInitEvent#addBootstrapListener(BootstrapListener)
 *
 * @deprecated Since 3.0, this API is deprecated in favor of
 *             {@link IndexHtmlRequestListener} when using client-side
 *             bootstrapping
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
@Deprecated
public interface BootstrapListener extends EventListener, Serializable {

    /**
     * Lets this listener make changes to the overall HTML document that will be
     * used as the initial HTML page, as well as the HTTP headers in the
     * response serving the initial HTML.
     *
     * @param response
     *            the bootstrap response that can be modified to cause change in
     *            the generate HTML and in the HTTP headers of the response.
     */
    void modifyBootstrapPage(BootstrapPageResponse response);

}
