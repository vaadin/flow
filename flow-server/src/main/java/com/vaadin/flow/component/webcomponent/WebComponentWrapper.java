/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import tools.jackson.databind.node.BaseJsonNode;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.webcomponent.WebComponentBinding;
import com.vaadin.flow.shared.Registration;

/**
 * Wrapper component for a web component that exposes {@link ClientCallable}
 * methods that the client-side components expect to be available.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd.
 * @since 2.0
 */
public class WebComponentWrapper extends Component {

    private WebComponentBinding<?> webComponentBinding;

    // Disconnect timeout
    private Registration disconnectRegistration;
    private long disconnect;

    /**
     * Wrapper class for the server side WebComponent.
     *
     * @param rootElement
     *            {@link Element} to which the {@code WebComponentWrapper} is
     *            bound to.
     * @param binding
     *            binding that offers methods for delivering property updates to
     *            the {@code component} being wrapped by
     *            {@code WebComponentWrapper}
     */
    public WebComponentWrapper(Element rootElement,
            WebComponentBinding<?> binding) {
        super(rootElement);
        Objects.requireNonNull(binding,
                "Parameter 'binding' must not be null!");

        webComponentBinding = binding;
        getElement().attachShadow()
                .appendChild(webComponentBinding.getComponent().getElement());
    }

    /**
     * Wrapper class for the server side WebComponent.
     *
     * @param rootElement
     *            {@link Element} to which the {@code WebComponentWrapper} is
     *            bound to.
     * @param binding
     *            binding that offers methods for delivering property updates to
     *            the {@code component} being wrapped by
     *            {@code WebComponentWrapper}
     * @param bootstrapElements
     *            elements that should be added to the shadow dom of the
     *            {@code rootElement}. These are copies of the original elements
     *            and the copies are created by
     *            {@link com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry}
     */
    protected WebComponentWrapper(Element rootElement,
            WebComponentBinding<?> binding, List<Element> bootstrapElements) {
        this(rootElement, binding);
        // shadow root is attached in this(...)
        getElement().getShadowRoot().ifPresent(shadow -> shadow
                .appendChild(bootstrapElements.toArray(new Element[0])));
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
    public void sync(String property, BaseJsonNode newValue) {
        try {
            webComponentBinding.updateProperty(property, newValue);
        } catch (IllegalArgumentException e) {
            LoggerFactory
                    .getLogger(webComponentBinding.getComponent().getClass())
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
                    webComponentBinding.getComponent().getClass().getName());
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
                            Element element = getElement();
                            element.getParent().removeVirtualChild(element);
                            disconnectRegistration.remove();
                        }
                    });
        }
    }
}
