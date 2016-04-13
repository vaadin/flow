/*
 * Copyright 2000-2016 Vaadin Ltd.
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

package com.vaadin.hummingbird.namespace;

import java.util.HashMap;
import java.util.stream.Stream;

import com.vaadin.hummingbird.NodeOwner;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.StateTree;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResourceRegistration;
import com.vaadin.server.VaadinSession;

/**
 * Namespace for element attribute values.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ElementAttributeNamespace extends MapNamespace {

    private final HashMap<String, StreamResourceRegistration> resourceRegistrations = new HashMap<>();

    private final HashMap<String, EventRegistrationHandle> resourceAttributeHandles = new HashMap<>();

    /**
     * Creates a new element attribute namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     */
    public ElementAttributeNamespace(StateNode node) {
        super(node);
        getNode().addDetachListener(this::unregisterResources);
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
        if (resourceAttributeHandles.containsKey(attribute)) {
            return false;
        }
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
        doRemove(attribute);
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
        if (resourceAttributeHandles.containsKey(attribute)) {
            throw new IllegalStateException("The node is not attached "
                    + "therefore resource URL which is the value "
                    + "for this attribute is not defined.");
        }
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
        if (getNode().isAttached()) {
            doSetResource(attribute, resource);
        } else {
            unregisterResource(attribute);
            EventRegistrationHandle handle = getNode().addAttachListener(
                    () -> doSetResource(attribute, resource));
            resourceAttributeHandles.put(attribute, handle);
        }
    }

    @Override
    public final StateNode getNode() {
        return super.getNode();
    }

    private void unregisterResources() {
        resourceRegistrations.entrySet().forEach(
                entry -> unsetResource(entry.getKey(), entry.getValue()));
    }

    private void unregisterResource(String attribute) {
        StreamResourceRegistration registration = resourceRegistrations
                .remove(attribute);
        EventRegistrationHandle handle = resourceAttributeHandles
                .remove(attribute);
        if (handle != null) {
            handle.remove();
        }
        if (registration != null) {
            registration.unregister();
        }
    }

    private void unsetResource(String attribute,
            StreamResourceRegistration registration) {
        if (registration != null) {
            registration.unregister();
        }
        doRemove(attribute);
    }

    private void doSetResource(String attribute, StreamResource resource) {
        NodeOwner owner = getNode().getOwner();
        assert owner instanceof StateTree;
        VaadinSession session = ((StateTree) owner).getUI().getSession();
        StreamResourceRegistration registration = session.getResourceRegistry()
                .registerResource(resource);
        set(attribute, registration.getResourceUri().toASCIIString());
        resourceRegistrations.put(attribute, registration);
    }

    private void doRemove(String attribute) {
        super.remove(attribute);
    }

}
