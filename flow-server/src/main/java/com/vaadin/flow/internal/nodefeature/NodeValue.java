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
package com.vaadin.flow.internal.nodefeature;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.EmptyChange;
import com.vaadin.flow.internal.change.MapPutChange;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.shared.util.UniqueSerializable;

/**
 * A node feature that carries a single value. Represented as a map containing
 * the key returned by {@link #getKey()} on the client.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <T>
 *            the type of the stored value
 */
public abstract class NodeValue<T extends Serializable> extends NodeFeature {
    // Null is ignored by Map.computeIfAbsent -> using a marker value instead
    private static final UniqueSerializable NULL_MARKER = new UniqueSerializable() {
        // empty
    };

    private T value;

    private boolean isPopulated;

    /**
     * Creates a new feature for the given node.
     *
     * @param node
     *            the node which supports the feature
     */
    public NodeValue(StateNode node) {
        super(node);
        isPopulated = !node.isReportedFeature(getClass());
    }

    /**
     * Gets the key that should be used when the value of this feature is sent
     * to the client.
     * <p>
     * The key is fetched on demand from the sub class instead of e.g. requiring
     * it as a constructor parameter to avoid storing an additional member field
     * in each instance.
     *
     * @return the key value, not <code>null</code>
     */
    protected abstract String getKey();

    /**
     * Sets the value of this feature.
     *
     * @param value
     *            the value to set
     */
    protected void setValue(T value) {
        if (!Objects.equals(value, this.value)) {
            markAsDirty();

            this.value = value;
        }
    }

    private void markAsDirty() {
        getNode().markAsDirty();

        // Store current value as the change tracker if not value is already
        // stored
        getNode().getChangeTracker(this,
                () -> this.value != null ? this.value : NULL_MARKER);
    }

    /**
     * Gets the value of this feature.
     *
     * @return the previously set value
     */
    protected T getValue() {
        return value;
    }

    @Override
    public void collectChanges(Consumer<NodeChange> collector) {
        Serializable originalValue = getNode().getChangeTracker(this,
                () -> null);
        assert originalValue != null;
        if (originalValue == NULL_MARKER) {
            originalValue = null;
        }

        if (!Objects.equals(originalValue, this.value)) {
            collector.accept(new MapPutChange(this, getKey(), value));
        } else if (!isPopulated) {
            collector.accept(new EmptyChange(this));
        }
        isPopulated = true;
    }

    @Override
    public void generateChangesFromEmpty() {
        getNode().getChangeTracker(this, () -> NULL_MARKER);
    }

    @Override
    public void forEachChild(Consumer<StateNode> action) {
        if (getValue() instanceof StateNode) {
            action.accept((StateNode) getValue());
        }
    }

}
