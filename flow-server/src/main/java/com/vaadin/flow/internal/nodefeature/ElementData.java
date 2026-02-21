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
package com.vaadin.flow.internal.nodefeature;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.node.BaseJsonNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.signals.Signal;

/**
 * Map of basic element information.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ElementData extends NodeMap {

    /**
     * Creates a new element data map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     *
     */
    public ElementData(StateNode node) {
        super(node);
    }

    @Override
    protected @Nullable Serializable get(String key) {
        Serializable value = super.get(key);
        if (value instanceof SignalBinding) {
            return ((SignalBinding) value).value();
        } else {
            return value;
        }
    }

    /**
     * Sets the tag name of the element.
     *
     * @param tag
     *            the tag name
     */
    public void setTag(String tag) {
        put(NodeProperties.TAG, tag);
    }

    /**
     * Gets the tag name of the element.
     *
     * @return the tag name
     */
    public @Nullable String getTag() {
        return getOrDefault(NodeProperties.TAG, null);
    }

    /**
     * Sets the namespace of the element.
     *
     * @param namespace
     *            the namespace to set
     */
    public void setNamespace(String namespace) {
        put(NodeProperties.NAMESPACE, namespace);
    }

    /**
     * Gets the namespace of the element.
     *
     * @return namespace
     */
    public @Nullable String getNamespace() {
        return getOrDefault(NodeProperties.NAMESPACE, null);
    }

    /**
     * Sets the payload data of the element.
     *
     * @param payload
     *            the payload data
     */
    public void setPayload(BaseJsonNode payload) {
        put(NodeProperties.PAYLOAD, payload);
    }

    /**
     * Set the visibility of the element.
     *
     * @param visible
     *            is the element visible or hidden
     */
    public void setVisible(boolean visible) {
        put(NodeProperties.VISIBLE, visible);
    }

    /**
     * Get element visibility.
     *
     * @return Element is visible by default
     */
    public boolean isVisible() {
        var value = get(NodeProperties.VISIBLE);
        return !Boolean.FALSE
                .equals(value instanceof SignalBinding signalBinding
                        ? signalBinding.value()
                        : value);
    }

    /**
     * Gets the payload data of the element.
     *
     * @return the payload data of the element
     */
    public @Nullable BaseJsonNode getPayload() {
        Serializable value = get(NodeProperties.PAYLOAD);
        return value == null ? null : (BaseJsonNode) value;
    }

    @Override
    public boolean allowsChanges() {
        return isVisible();
    }

    public void setJavaClass(Class<? extends Component> componentClass) {
        put(NodeProperties.JAVA_CLASS, componentClass.getName());
    }

    public @Nullable String getJavaClass() {
        return getOrDefault(NodeProperties.JAVA_CLASS, null);
    }

    /**
     * Binds the given signal to the <code>visible</code> property.
     *
     * @param owner
     *            the element owning the property, not <code>null</code>
     * @param signal
     *            the signal to bind, not <code>null</code>
     * @throws com.vaadin.flow.signals.BindingActiveException
     *             thrown when there is already an existing binding for the
     *             <code>visible</code> property
     */
    public void bindVisibleSignal(Element owner, Signal<Boolean> signal) {
        bindSignal(owner, NodeProperties.VISIBLE, signal,
                (element, value) -> putVisibleSignalValue(value), null);
    }

    private void putVisibleSignalValue(@Nullable Boolean value) {
        boolean booleanValue = (value != null) ? value : Boolean.FALSE;
        if (hasSignal(NodeProperties.VISIBLE) && super.get(
                NodeProperties.VISIBLE) instanceof SignalBinding b) {
            put(NodeProperties.VISIBLE, new SignalBinding(b.signal(),
                    b.registration(), booleanValue, null));
        } else {
            put(NodeProperties.VISIBLE, booleanValue);
        }
    }
}
