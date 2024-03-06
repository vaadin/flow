/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication;

import java.io.Serializable;
import java.util.EventListener;

import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;

/**
 * This event listener is notified when the Single Page Application's HTML page
 * is about to be generated and sent to the client. The Index HTML response is
 * first constructed as an in-memory DOM representation which registered
 * listeners can modify before the final HTML is generated.
 * <p>
 * Index HTML request listeners are registered using the
 * {@link ServiceInitEvent} during the initialization of the application. Index
 * HTML request listener is used when the application flag
 * {@link InitParameters#SERVLET_PARAMETER_USE_V14_BOOTSTRAP} is not set.
 *
 * @see ServiceInitEvent#addIndexHtmlRequestListener(IndexHtmlRequestListener)
 * @see IndexHtmlRequestHandler
 * @see InitParameters#SERVLET_PARAMETER_USE_V14_BOOTSTRAP
 */
@FunctionalInterface
public interface IndexHtmlRequestListener extends EventListener, Serializable {
    /**
     * The method allows to modify the Index HTML response before it is sent to
     * browser.
     *
     * @param indexHtmlResponse
     *            the response object which includes the {@link VaadinRequest},
     *            {@link VaadinResponse}, and {@link org.jsoup.nodes.Document}
     */
    void modifyIndexHtmlResponse(IndexHtmlResponse indexHtmlResponse);
}
