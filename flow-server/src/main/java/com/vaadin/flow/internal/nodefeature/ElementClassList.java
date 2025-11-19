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
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.vaadin.flow.dom.ClassList;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementEffect;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.EmptyChange;
import com.vaadin.flow.internal.change.ListAddChange;
import com.vaadin.flow.internal.change.ListClearChange;
import com.vaadin.flow.internal.change.NodeChange;
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
public class ElementClassList extends SerializableNodeList<Serializable> {

    private record SignalBinding(Signal<Boolean> signal,
            Registration registration, String name) implements Serializable {
    }

    private static class ClassListView extends AbstractSet<String>
            implements ClassList {

        private final ElementClassList elementClassList;

        private ClassListView(ElementClassList elementClassList) {
            this.elementClassList = elementClassList;
        }

        @Override
        public int size() {
            return elementClassList.getStringsSnapshot().size();
        }

        @Override
        public Iterator<String> iterator() {
            List<String> snapshot = elementClassList.getStringsSnapshot();
            return snapshot.iterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof String s)) {
                return false;
            }
            return elementClassList.indexOfString(s) != -1;
        }

        @Override
        public boolean add(String className) {
            validate(className);
            if (isBound(className)) {
                throw new BindingActiveException("Class name '" + className
                        + "' is bound and cannot be modified manually");
            }
            if (contains(className)) {
                return false;
            }
            elementClassList.add(elementClassList.size(), className);
            return true;
        }

        @Override
        public boolean remove(Object className) {
            if (!(className instanceof String name)) {
                return false;
            }
            if (isBound(name)) {
                throw new BindingActiveException("Class name '" + name
                        + "' is bound and cannot be modified manually");
            }
            int mixedIndex = elementClassList.indexOfString(name);
            if (mixedIndex == -1) {
                return false;
            }
            elementClassList.remove(mixedIndex);
            return true;
        }

        @Override
        public void clear() {
            clearBindings();
            elementClassList.clear();
        }

        @Override
        public void bind(String name, Signal<Boolean> signal) {
            validate(name);
            if (signal == null) {
                SignalBinding old = elementClassList.removeBinding(name);
                if (old != null && old.registration != null) {
                    old.registration.remove();
                }
                return;
            }

            SignalBinding existing = elementClassList.removeBinding(name);
            if (existing != null && existing.registration != null) {
                existing.registration.remove();
            }
            Element owner = Element.get(getNode());
            Registration registration = ElementEffect.bind(owner, signal,
                    (element, value) -> internalSetPresence(name,
                            Boolean.TRUE.equals(value)));
            elementClassList.addBinding(new SignalBinding(signal, registration, name));
        }

        private void validate(String className) {
            if (className == null) {
                throw new IllegalArgumentException("Class name cannot be null");
            }
            if (className.isEmpty()) {
                throw new IllegalArgumentException(
                        "Class name cannot be empty");
            }
            if (className.indexOf(' ') != -1) {
                throw new IllegalArgumentException(
                        "Class name cannot contain spaces");
            }
        }

        private void internalSetPresence(String name, boolean set) {
            int index = elementClassList.indexOfString(name);
            if (set) {
                if (index == -1) {
                    elementClassList.add(elementClassList.size(), name);
                }
            } else {
                if (index != -1) {
                    elementClassList.remove(index);
                }
            }
        }

        private boolean isBound(String name) {
            return elementClassList.isBound(name);
        }

        private void clearBindings() {
            elementClassList.clearBindings();
        }

        private StateNode getNode() {
            return elementClassList.getNode();
        }
    }

    // Snapshot of last Strings-only state emitted to the client
    private List<String> previousStrings;

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

    // ---- Binding management stored inline as list entries ----

    private int findBindingIndex(String name) {
        int n = size();
        for (int i = 0; i < n; i++) {
            Serializable v = get(i);
            if (v instanceof SignalBinding b && b.name().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isBound(String name) {
        return findBindingIndex(name) != -1;
    }

    private void addBinding(SignalBinding binding) {
        // Store bindings at the end to minimize interference with class order
        add(size(), binding);
    }

    private SignalBinding removeBinding(String name) {
        int idx = findBindingIndex(name);
        if (idx != -1) {
            Serializable v = remove(idx);
            return (SignalBinding) v;
        }
        return null;
    }

    private void clearBindings() {
        // Remove all SignalBinding entries and unregister
        for (int i = size() - 1; i >= 0; i--) {
            Serializable v = get(i);
            if (v instanceof SignalBinding b) {
                if (b.registration() != null) {
                    b.registration().remove();
                }
                remove(i);
            }
        }
    }

    // ---- Helper methods for String entries ----

    private int indexOfString(String name) {
        int n = size();
        for (int i = 0; i < n; i++) {
            Serializable v = get(i);
            if (v instanceof String s && s.equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private List<String> getStringsSnapshot() {
        List<String> out = new ArrayList<>();
        int n = size();
        for (int i = 0; i < n; i++) {
            Serializable v = get(i);
            if (v instanceof String s) {
                out.add(s);
            }
        }
        return out;
    }

    private boolean hasBindings() {
        int n = size();
        for (int i = 0; i < n; i++) {
            if (get(i) instanceof SignalBinding) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void collectChanges(
            java.util.function.Consumer<NodeChange> collector) {
        if (!hasBindings()) {
            // Use default behavior when there are no inline bindings
            super.collectChanges(collector);
            return;
        }
        List<String> current = getStringsSnapshot();
        if (previousStrings == null) {
            if (current.isEmpty()) {
                collector.accept(new EmptyChange(this));
            } else {
                collector.accept(new ListAddChange<>(this, false, 0,
                        new ArrayList<>(current)));
            }
            previousStrings = new ArrayList<>(current);
            return;
        }

        // If no change in the strings view, emit nothing
        if (previousStrings.equals(current)) {
            return;
        }

        // For simplicity and correctness, emit clear + add when anything
        // changes
        collector.accept(new ListClearChange<>(this));
        if (!current.isEmpty()) {
            collector.accept(new ListAddChange<>(this, false, 0,
                    new ArrayList<>(current)));
        }
        previousStrings = new ArrayList<>(current);
    }

    @Override
    public void generateChangesFromEmpty() {
        if (!hasBindings()) {
            super.generateChangesFromEmpty();
        } else {
            // No-op: this feature reports its list state through a custom
            // collectChanges implementation which emits either EmptyChange or
            // full add list as needed for Strings only.
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Reset snapshot so that after re-attach we emit initial state again
        previousStrings = null;
    }
}
