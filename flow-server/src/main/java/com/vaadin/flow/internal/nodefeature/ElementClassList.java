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

import java.util.Collection;

import com.vaadin.flow.dom.ClassList;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.Signal;

/**
 * Handles CSS class names for an element.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ElementClassList extends SerializableNodeList<String> {

    private Signal<String> signal;

    private Registration signalRegistration;

    private boolean signalRemovalEnabled = true;

    private static class ClassListView extends NodeList.SetView<String>
            implements ClassList {

        private ClassListView(ElementClassList elementClassList) {
            super(elementClassList);
        }

        @Override
        protected void validate(String className) {
            if (className == null) {
                throw new IllegalArgumentException("Class name cannot be null");
            }

            if ("".equals(className)) {
                throw new IllegalArgumentException(
                        "Class name cannot be empty");
            }
            if (className.indexOf(' ') != -1) {
                throw new IllegalArgumentException(
                        "Class name cannot contain spaces");
            }
        }
    }

    /**
     * Creates a new class list for the given node.
     *
     * @param node
     *            the node that the list belongs to
     */
    public ElementClassList(StateNode node) {
        super(node);
    }

    /**
     * Creates a view into this list.
     *
     * @return a view into this list
     */
    public ClassList getClassList() {
        return new ClassListView(this);
    }

    /**
     * Binds the given signal to this list. <code>null</code> signal unbinds
     * existing binding.
     *
     * @param signal
     *            the signal to bind or <code>null</code> to unbind any existing
     *            binding
     * @param bindAction
     *            the action to perform the binding, may be <code>null</code>
     */
    public void bindSignal(Signal<String> signal,
            SerializableSupplier<Registration> bindAction) {
        var previousSignal = this.signal;
        if (signal != null && previousSignal != null) {
            throw new BindingActiveException("Binding is already active");
        }
        Registration registration = bindAction != null ? bindAction.get()
                : null;
        if (registration != null) {
            signalRegistration = registration;
        }
        if (signal == null && signalRegistration != null) {
            signalRegistration.remove();
            this.signal = null;
        } else {
            this.signal = signal;
        }
    }

    public Signal<String> getSignal() {
        return signal;
    }

    @Override
    protected void clear() {
        removeSignal();
        super.clear();
    }

    @Override
    protected void add(String item) {
        removeSignal();
        super.add(item);
    }

    @Override
    protected void add(int index, String item) {
        removeSignal();
        super.add(index, item);
    }

    @Override
    protected void addAll(Collection<? extends String> items) {
        removeSignal();
        super.addAll(items);
    }

    @Override
    protected String remove(int index) {
        removeSignal();
        return super.remove(index);
    }

    /**
     * Sets whether signal removal is enabled. When signal removal is enabled,
     * the signal binding is removed when the list is modified. Enabled by
     * default.
     *
     * @param signalRemovalEnabled
     *            <code>true</code> to enable signal removal, <code>false</code>
     *            to disable it
     */
    public void setSignalRemovalEnabled(boolean signalRemovalEnabled) {
        this.signalRemovalEnabled = signalRemovalEnabled;
    }

    private void removeSignal() {
        if (signalRemovalEnabled && getSignal() != null) {
            bindSignal(null, null);
        }
    }
}
