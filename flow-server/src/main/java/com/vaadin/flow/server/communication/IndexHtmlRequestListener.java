/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.flow.server.communication;

import java.io.Serializable;
import java.util.EventListener;

import com.vaadin.flow.server.Constants;
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
 * {@link Constants#SERVLET_PARAMETER_USE_V14_BOOTSTRAP} is not set.
 *
 * @see ServiceInitEvent#addIndexHtmlRequestListener(IndexHtmlRequestListener)
 * @see IndexHtmlRequestHandler
 * @see Constants#SERVLET_PARAMETER_USE_V14_BOOTSTRAP
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
