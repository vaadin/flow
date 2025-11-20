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
package com.vaadin.flow.internal.nodefeature;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementEffect;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.NodeOwner;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.Signal;

/**
 * Map for element attribute values.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ElementAttributeMap extends NodeMap {

    private Map<String, StreamRegistration> resourceRegistrations;

    private Map<String, Registration> pendingRegistrations;

    /**
     * Creates a new element attribute map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     */
    public ElementAttributeMap(StateNode node) {
        super(node);
    }

    /**
     * Sets the given attribute to the given value.
     *
     * @param attribute
     *            the attribute name
     * @param value
     *            the value
     */
    public void set(String attribute, String value) {
        if (hasSignal(attribute)) {
            throw new BindingActiveException(
                    "setAttribute is not allowed while a binding for the given attribute exists.");
        }
        doSet(attribute, value);
    }

    /**
     * Binds the given signal to the given attribute. <code>null</code> signal
     * unbinds existing binding.
     *
     * @param owner
     *            the element owning the attribute, not <code>null</code>
     * @param attribute
     *            the name of the attribute
     * @param signal
     *            the signal to bind or <code>null</code> to unbind any existing
     *            binding
     */
    public void bindSignal(Element owner, String attribute,
            Signal<String> signal) {
        SignalBinding previousSignalBinding;
        if (super.get(attribute) instanceof SignalBinding binding) {
            previousSignalBinding = binding;
        } else {
            previousSignalBinding = null;
        }
        if (signal != null && previousSignalBinding != null
                && previousSignalBinding.signal() != null) {
            throw new BindingActiveException();
        }

        Registration registration = signal != null ? ElementEffect.bind(owner,
                signal, (element, value) -> doSet(attribute, value)) : null;
        if (signal == null && previousSignalBinding != null) {
            if (previousSignalBinding.registration() != null) {
                previousSignalBinding.registration().remove();
            }
            put(attribute, get(attribute), false);
        } else {
            put(attribute,
                    new SignalBinding(signal, registration, attribute, get(attribute)),
                    false);
        }
    }

    /**
     * Checks whether an attribute with the given name has been set.
     *
     * @param attribute
     *            the name of the attribute
     * @return <code>true</code> if there is a property with the given name;
     *         <code>false</code> if there is no property
     */
    public boolean has(String attribute) {
        if (contains(attribute)) {
            if (hasSignal(attribute)) {
                SignalBinding binding = (SignalBinding) super.get(attribute);
                return binding.value() != null;
            }
            return true;
        }
        return false;
    }

    private boolean hasSignal(String attribute) {
        Serializable value = super.get(attribute);
        return value instanceof SignalBinding binding
                && binding.signal() != null;
    }

    /**
     * Removes the named attribute.
     *
     * @param attribute
     *            the name of the attribute to remove
     */
    @Override
    public Serializable remove(String attribute) {
        if (hasSignal(attribute)) {
            throw new BindingActiveException(
                    "removeAttribute is not allowed while a binding for the given attribute exists.");
        }
        unregisterResource(attribute);
        return super.remove(attribute);
    }

    /**
     * Gets the value of an attribute.
     *
     * @param attribute
     *            the name of the property
     * @return the attribute value or <code>null</code> if the attribute has not
     *         been set
     */
    @Override
    public String get(String attribute) {
        Serializable value = super.get(attribute);
        if (value == null || value instanceof String) {
            return (String) value;
        } else if (value instanceof JsonNode node) {
            // The only object which may be set by the current imlp contains
            // "uri" attribute, only this situation is expected here.
            assert node.has(NodeProperties.URI_ATTRIBUTE);
            return node.get(NodeProperties.URI_ATTRIBUTE).asString();
        } else {
            // If the value is not a string or JsonNode then current impl only
            // uses SignalBinding
            assert value instanceof SignalBinding;
            return (String) ((SignalBinding) value).value();
        }
    }

    /**
     * Gets the attribute names.
     *
     * @return a stream of all the attribute names which have been set
     */
    public Stream<String> attributes() {
        return super.keySet().stream();
    }

    /**
     * Sets the given attribute to the given {@link StreamResource} value.
     *
     * @param attribute
     *            the attribute name
     * @param resource
     *            the value
     */
    public void setResource(String attribute, AbstractStreamResource resource) {
        doSetResource(attribute, resource);
        if (getNode().isAttached()) {
            registerResource(attribute, resource);
        } else {
            deferRegistration(attribute, resource);
        }
    }

    private void doSetResource(String attribute,
            AbstractStreamResource resource) {
        final URI targetUri;
        if (VaadinSession.getCurrent() != null) {
            final StreamResourceRegistry resourceRegistry = VaadinSession
                    .getCurrent().getResourceRegistry();
            targetUri = resourceRegistry.getTargetURI(resource);
        } else {
            targetUri = StreamResourceRegistry.getURI(resource);
        }
        ObjectNode object = JacksonUtils.createObjectNode();
        object.put(NodeProperties.URI_ATTRIBUTE, targetUri.toASCIIString());
        // don't use string as a value, but wrap it into an object to let know
        // the client side about specific nature of the value
        doSet(attribute, object);
    }

    private void ensurePendingRegistrations() {
        if (pendingRegistrations == null) {
            pendingRegistrations = new HashMap<>();
        }
    }

    private void ensureResourceRegistrations() {
        if (resourceRegistrations == null) {
            resourceRegistrations = new HashMap<>();
        }
    }

    private void unregisterResource(String attribute) {
        ensureResourceRegistrations();
        ensurePendingRegistrations();

        StreamRegistration registration = resourceRegistrations
                .remove(attribute);
        Registration handle = pendingRegistrations.remove(attribute);
        if (handle != null) {
            handle.remove();
        }
        if (registration != null) {
            registration.unregister();
        }
        if (resourceRegistrations.isEmpty()) {
            resourceRegistrations = null;
        }
        if (pendingRegistrations.isEmpty()) {
            pendingRegistrations = null;
        }
    }

    private void deferRegistration(String attribute,
            AbstractStreamResource resource) {
        ensurePendingRegistrations();

        assert !pendingRegistrations.containsKey(attribute);
        Registration handle = getNode()
                // This explicit class instantiation is the workaround
                // which fixes a JVM optimization+serialization bug.
                // Do not convert to lambda
                // Detected under Win7_64 /JDK 1.8.0_152, 1.8.0_172
                .addAttachListener(new Command() {
                    @Override
                    public void execute() {
                        doSetResource(attribute, resource);
                        registerResource(attribute, resource);
                    }
                });
        pendingRegistrations.put(attribute, handle);
    }

    private void registerResource(String attribute,
            AbstractStreamResource resource) {
        ensureResourceRegistrations();
        ensurePendingRegistrations();

        assert !resourceRegistrations.containsKey(attribute);
        StreamRegistration registration = getSession().getResourceRegistry()
                .registerResource(resource);
        resourceRegistrations.put(attribute, registration);
        Registration handle = pendingRegistrations.remove(attribute);
        if (handle != null) {
            handle.remove();
        }
        pendingRegistrations.put(attribute, getNode().addDetachListener(
                // This explicit class instantiation is the workaround
                // which fixes a JVM optimization+serialization bug.
                // Do not convert to lambda
                // Detected under Win7_64 /JDK 1.8.0_152, 1.8.0_172
                // see ElementAttributeMap#deferRegistration
                new Command() {
                    @Override
                    public void execute() {
                        ElementAttributeMap.this.unsetResource(attribute);
                    }
                }));
    }

    private void doSet(String attribute, Serializable value) {
        unregisterResource(attribute);
        if (hasSignal(attribute)) {
            SignalBinding binding = (SignalBinding) super.get(attribute);
            put(attribute, new SignalBinding(binding.signal(),
                    binding.registration(), attribute, value));
        } else if (value == null) {
            super.remove(attribute);
        } else {
            put(attribute, value);
        }
    }

    private void unsetResource(String attribute) {
        ensureResourceRegistrations();
        StreamRegistration registration = resourceRegistrations.get(attribute);
        Optional<AbstractStreamResource> resource = Optional.empty();
        if (registration != null) {
            resource = Optional.ofNullable(registration.getResource());
        }
        unregisterResource(attribute);
        resource.ifPresent(res -> deferRegistration(attribute, res));
    }

    private VaadinSession getSession() {
        NodeOwner owner = getNode().getOwner();
        assert owner instanceof StateTree;
        return ((StateTree) owner).getUI().getSession();
    }

}
