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

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JsonCodec;
import com.vaadin.flow.shared.Registration;

import elemental.json.JsonValue;

/**
 * Wrapper component for a WebComponent that exposes client callable methods
 * that the client side components expect to be available.
 */
public class WebComponentWrapper extends Component implements WebComponentProxy {

    private Component child;
    private WebComponentBinding<? extends Serializable> webComponentBinding;

    // Disconnect timeout
    private Registration disconnectRegistration;
    private long disconnect;

    /**
     * Wrapper class for the server side WebComponent.
     *
     * @param tag
     *         web component tag
     */
    public WebComponentWrapper(String tag) {
        super(new Element(tag));

    }

    /**
     * Synchronize method for client side to send property value updates to the
     * server.
     *
     * @param property
     *         property name to update
     * @param newValue
     *         the new value to set
     */
    @ClientCallable
    public void sync(String property, JsonValue newValue) {
        try {
            if (webComponentBinding.hasProperty(property)) {
                setNewPropertyValue(property, newValue);
            } else {
                LoggerFactory.getLogger(child.getClass())
                        .error("No match found for {}", property);
            }
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
            LoggerFactory.getLogger(WebComponentUI.class)
                    .warn("Received reconnect request for non disconnected WebComponent '{}'",
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
                                .getLastHeartbeatTimestamp() - disconnect
                                > timeout) {
                            this.getElement().removeFromParent();
                        }
                    });
        }
    }

    private void setNewPropertyValue(String property, JsonValue newValue) {
        checkProxyState();

        Class<? extends Serializable> propertyType =
                webComponentBinding.getPropertyType(property);

        if (JsonCodec.canEncodeWithoutTypeInfo(propertyType)) {
            Serializable value = JsonCodec.decodeAs(newValue, propertyType);
            webComponentBinding.updateProperty(property, value);
        } else {
            throw new IllegalArgumentException(
                    String.format("Received value was not convertible to '%s'",
                            propertyType.getName()));
        }
    }

    @Override
    public void setWebComponentBinding(WebComponentBinding<? extends Component> binding) {
        Objects.requireNonNull(binding,"Parameter 'binding' must not be null!");

        if (this.webComponentBinding != null) {
            throw new IllegalStateException("Binding has already been set and" +
                    " cannot be set again");
        }

        this.webComponentBinding = binding;
        this.child = webComponentBinding.getComponent();
        getElement().appendChild(child.getElement());
    }

    private void checkProxyState() {
        if (webComponentBinding == null) {
            throw new IllegalStateException(String.format("%s has not been " +
                            "set for the %s implementation!",
                    WebComponentBinding.class.getSimpleName(),
                    WebComponentProxy.class.getSimpleName()));
        }

        if (child == null) {
            throw new IllegalStateException(String.format("%s is missing the " +
                            "proxy target - no child is set.",
                    WebComponentProxy.class.getSimpleName()));
        }
    }

    /**
     * For testing purposes only.
     * @return  web component binding
     */
    protected WebComponentBinding getWebComponentBinding() {
        return webComponentBinding;
    }
}
