/*
 * Copyright 2000-2017 Vaadin Ltd.
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

package com.vaadin.flow.nodefeature;

import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.flow.NodeOwner;
import com.vaadin.flow.StateNode;
import com.vaadin.flow.StateTree;
import com.vaadin.server.StreamReceiver;
import com.vaadin.server.StreamReceiverRegistration;
import com.vaadin.server.StreamReceiverRegistry;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResourceRegistration;
import com.vaadin.server.StreamResourceRegistry;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.Registration;

/**
 * Map for element attribute values.
 *
 * @author Vaadin Ltd
 */
public class ElementAttributeMap extends NodeMap {

    private HashMap<String, StreamResourceRegistration> resourceRegistrations;
    private HashMap<String, StreamReceiverRegistration> receiverRegistrations;

    private HashMap<String, Registration> pendingRegistrations;

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
        unregisterResource(attribute);
        put(attribute, value);
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
        return contains(attribute);
    }

    /**
     * Removes the named attribute.
     *
     * @param attribute
     *            the name of the attribute to remove
     */
    @Override
    public void remove(String attribute) {
        unregisterResource(attribute);
        super.remove(attribute);
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
        return (String) super.get(attribute);
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
    public void setResource(String attribute, StreamResource resource) {
        set(attribute, StreamResourceRegistry.getURI(resource).toASCIIString());
        if (getNode().isAttached()) {
            registerResource(attribute, resource);
        } else {
            deferRegistration(attribute, resource);
        }
    }

    public void setResource(String attribute, StreamReceiver receiver) {
        set(attribute, StreamReceiverRegistry.getURI(receiver).toASCIIString());
        if (getNode().isAttached()) {
            registerResource(attribute, receiver);
        } else {
            deferRegistration(attribute, receiver);
        }
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

    private void ensureReceiverRegistrations() {
        if (receiverRegistrations == null) {
            receiverRegistrations = new HashMap<>();
        }
    }

    private void unregisterResource(String attribute) {
        ensureResourceRegistrations();
        ensurePendingRegistrations();

        StreamResourceRegistration registration = resourceRegistrations
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

    private void deferRegistration(String attribute, StreamResource resource) {
        ensurePendingRegistrations();

        assert !pendingRegistrations.containsKey(attribute);
        Registration handle = getNode()
                .addAttachListener(() -> registerResource(attribute, resource));
        pendingRegistrations.put(attribute, handle);
    }

    private void registerResource(String attribute, StreamResource resource) {
        ensureResourceRegistrations();
        ensurePendingRegistrations();

        assert !resourceRegistrations.containsKey(attribute);
        StreamResourceRegistration registration = getSession()
                .getResourceRegistry().registerResource(resource);
        resourceRegistrations.put(attribute, registration);
        Registration handle = pendingRegistrations.remove(attribute);
        if (handle != null) {
            handle.remove();
        }
        pendingRegistrations.put(attribute,
                getNode().addDetachListener(() -> unsetResource(attribute)));
    }

    private void deferRegistration(String attribute, StreamReceiver resource) {
        ensurePendingRegistrations();

        assert !pendingRegistrations.containsKey(attribute);
        Registration handle = getNode()
                .addAttachListener(() -> registerResource(attribute, resource));
        pendingRegistrations.put(attribute, handle);
    }

    private void registerResource(String attribute, StreamReceiver resource) {
        ensureReceiverRegistrations();
        ensurePendingRegistrations();

        assert !resourceRegistrations.containsKey(attribute);
        StreamReceiverRegistration registration = getSession()
                .getReceiverRegistry().registerResource(resource);
        receiverRegistrations.put(attribute, registration);
        Registration handle = pendingRegistrations.remove(attribute);
        if (handle != null) {
            handle.remove();
        }
        pendingRegistrations.put(attribute,
                getNode().addDetachListener(() -> unsetResource(attribute)));
    }

    private void unsetResource(String attribute) {
        ensureResourceRegistrations();
        StreamResourceRegistration registration = resourceRegistrations
                .get(attribute);
        Optional<StreamResource> resource = Optional.empty();
        if (registration != null) {
            resource = registration.getResource();
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
