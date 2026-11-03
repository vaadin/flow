/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceInitEvent;

/**
 * A push transport provided by an add-on, for applications that want to push
 * with something else than the built-in transports based on Atmosphere.
 * <p>
 * A transport has three parts, which the add-on provides:
 * <ul>
 * <li>this object, registered with
 * {@link ServiceInitEvent#addPushTransport(PushTransport)} from a
 * {@link com.vaadin.flow.server.VaadinServiceInitListener}, which creates the
 * server side {@link PushConnection} of each UI using the transport,</li>
 * <li>the request handling of the transport, usually a
 * {@link com.vaadin.flow.server.RequestHandler} registered from the same
 * listener, which serves requests to {@code VAADIN/push} with a request type of
 * its own,</li>
 * <li>the client side of the transport, a script the client engine loads from
 * {@link #getClientScript()}.</li>
 * </ul>
 * The script registers the transport under its {@linkplain #getName() name} in
 * {@code window.Vaadin.Flow.pushTransports}, as an object with the subset of
 * the Atmosphere JavaScript API that the client engine uses:
 * {@code subscribe(request)}, which opens the connection described by the
 * request and calls its {@code onOpen}, {@code onMessage}, {@code onReopen},
 * {@code onReconnect}, {@code onError} and {@code onClose} callbacks, and
 * {@code unsubscribeUrl(url)}, which closes the connection opened for the url.
 * Messages to the server are sent as regular requests, as with the
 * {@code WEBSOCKET_XHR} transport.
 * <p>
 * The transport is selected with
 * {@link com.vaadin.flow.component.PushConfiguration#setTransportName(String)}
 * or {@link com.vaadin.flow.component.page.Push#transportName()}. When
 * Atmosphere is not on the classpath, push uses the first registered transport
 * instead of the built-in ones, so an application can leave out the
 * {@code flow-push} module and add the add-on without other changes.
 * <p>
 * Vaadin keeps the contract of this interface, but not the code of the
 * transports implementing it: the authors of an add-on are responsible for it.
 *
 * @author Vaadin Ltd
 */
public interface PushTransport extends Serializable {

    /**
     * Gets the name of this transport, which selects it in the push
     * configuration and is passed to the client as the transport. Must not be
     * the identifier of a built-in transport, such as {@code websocket}.
     *
     * @return the name of the transport, not {@code null}
     */
    String getName();

    /**
     * Creates the server side push connection of the given UI.
     *
     * @param ui
     *            the UI that pushes with this transport
     * @return the push connection, not {@code null}
     */
    PushConnection createPushConnection(UI ui);

    /**
     * Gets the URL of the script implementing the client side of this
     * transport, relative to the service URL, such as
     * {@code VAADIN/static/my-transport/my-transport.js}. Scripts in
     * {@code META-INF/resources/VAADIN/static} of a jar are served by Vaadin in
     * every deployment.
     *
     * @return the URL of the client script, not {@code null}
     */
    String getClientScript();
}
