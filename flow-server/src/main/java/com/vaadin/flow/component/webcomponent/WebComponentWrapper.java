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
package com.vaadin.flow.component.webcomponent;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.webcomponent.WebComponentBinding;
import com.vaadin.flow.shared.Registration;

import elemental.json.JsonValue;

/**
 * Wrapper component for a web component that exposes {@link ClientCallable}
 * methods that the client-side components expect to be available.
 *
 * @author Vaadin Ltd.
 * @since 2.0
 */
public class WebComponentWrapper extends Component {

    private Component child;
    private WebComponentBinding<?> webComponentBinding;

    // Disconnect timeout
    private Registration disconnectRegistration;
    private long disconnect;

    /**
     * Wrapper class for the server side WebComponent.
     *
     * @param rootElement
     *         {@link Element} to which the {@code WebComponentWrapper} is bound
     *         to.
     * @param binding
     *         binding that offers methods for delivering property updates to
     *         the {@code component} being wrapped by {@code
     *         WebComponentWrapper}
     */
    public WebComponentWrapper(Element rootElement,
                               WebComponentBinding<?> binding) {
        super(rootElement);
        Objects.requireNonNull(binding, "Parameter 'binding' must not be null!");

        this.webComponentBinding = binding;
        this.child = webComponentBinding.getComponent();
        getElement().appendChild(child.getElement());
    }

    /**
     * Synchronize method for client side to send property value updates to the
     * server.
     *
     * @param property
     *            property name to update
     * @param newValue
     *            the new value to set
     */
    @ClientCallable
    public void sync(String property, JsonValue newValue) {
        try {
            webComponentBinding.updateProperty(property, newValue);
        } catch (IllegalArgumentException e) {
            LoggerFactory.getLogger(child.getClass())
                    .error("Failed to synchronise property '{}'", property, e);
        }
    }

    /**
     * Cancel cleanup for a disconnected component.
     */
    @ClientCallable
    public void reconnect() {
        if (disconnectRegistration != null) {
            disconnectRegistration.remove();
        } else {
            LoggerFactory.getLogger(WebComponentUI.class).warn(
                    "Received reconnect request for non disconnected WebComponent '{}'",
                    this.child.getClass().getName());
        }
    }

    /**
     * A WebComponent disconnected from the dom will be scheduled for cleaning
     * if it doesn't get reconnected before times up.
     */
    @ClientCallable
    public void disconnected() {
        Optional<UI> uiOptional = getUI();

        if (uiOptional.isPresent() && disconnectRegistration == null) {
            disconnect = System.currentTimeMillis();
            disconnectRegistration = uiOptional.get().getInternals()
                    .addHeartbeatListener(event -> {
                        int disconnectTimeout = event.getSource().getSession()
                                .getConfiguration().getWebComponentDisconnect();

                        int timeout = 1000 * disconnectTimeout;

                        if (event.getSource().getInternals()
                                .getLastHeartbeatTimestamp()
                                - disconnect > timeout) {
                            Element element = this.getElement();
                            element.getParent().removeVirtualChild(element);
                        }
                    });
        }
    }
}
