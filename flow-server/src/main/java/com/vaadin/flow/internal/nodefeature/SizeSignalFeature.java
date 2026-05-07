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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Size;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Holds the lazily-allocated {@link ValueSignal} that backs
 * {@link Element#sizeSignal()} and tracks whether the per-UI resize observer
 * has been wired up for the element. Used to keep the same signal instance
 * across multiple {@code sizeSignal()} calls on a given element, and to ensure
 * the attach/detach observer registration happens only once.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@NullMarked
public class SizeSignalFeature extends ServerSideFeature {

    private @Nullable ValueSignal<Size> signal;
    private boolean observerRegistered;

    /**
     * Creates a SizeSignalFeature for the given node.
     *
     * @param node
     *            the node which supports the feature
     */
    public SizeSignalFeature(StateNode node) {
        super(node);
    }

    /**
     * Returns the underlying value signal, allocating it on first access with a
     * default value of {@code Size(0, 0)}.
     */
    public ValueSignal<Size> getOrCreateSignal() {
        if (signal == null) {
            signal = new ValueSignal<>(new Size(0, 0));
        }
        return signal;
    }

    /**
     * Returns whether the observer registration listeners have already been
     * installed for this element.
     */
    public boolean isObserverRegistered() {
        return observerRegistered;
    }

    /**
     * Marks the observer registration listeners as installed.
     */
    public void markObserverRegistered() {
        this.observerRegistered = true;
    }
}
