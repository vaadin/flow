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

import java.util.Optional;

import com.vaadin.flow.dom.ClassList;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementEffect;
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

    private static class ClassListView extends NodeList.SetView<String>
            implements ClassList {

        private final ElementClassList elementClassList;

        private ClassListView(ElementClassList elementClassList) {
            super(elementClassList);
            this.elementClassList = elementClassList;
        }

        @Override
        protected void validate(String className) {
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
            // Directly mutate the underlying NodeList to bypass SetView
            // add/remove overrides which enforce BindingActiveException for
            // manual updates.
            ElementClassList list = this.elementClassList;
            int index = list.indexOf(name);
            if (set) {
                if (index == -1) {
                    // append at the end
                    list.add(list.size(), name);
                }
            } else {
                if (index != -1) {
                    list.remove(index);
                }
            }
        }

        private StateNode getNode() {
            return elementClassList.getNode();
        }

        @Override
        public void bind(String name, Signal<Boolean> signal) {
            validate(name);
            SignalBindingFeature feature = getNode()
                    .getFeature(SignalBindingFeature.class);

            if (signal == null) {
                feature.removeBinding(SignalBindingFeature.CLASSES + name);
            } else {
                if (feature.hasBinding(SignalBindingFeature.CLASSES + name)) {
                    throw new BindingActiveException("Class name '" + name
                            + "' is already bound to a signal");
                }

                Registration registration = ElementEffect.bind(
                        Element.get(getNode()), signal,
                        (element, value) -> internalSetPresence(name,
                                Boolean.TRUE.equals(value)));
                feature.setBinding(SignalBindingFeature.CLASSES + name,
                        registration, signal);
            }
        }

        @Override
        public boolean add(String className) {
            throwIfBound(className);
            return super.add(className);
        }

        @Override
        public boolean remove(Object className) {
            if (className instanceof String name) {
                throwIfBound(name);
            }
            return super.remove(className);
        }

        @Override
        public void clear() {
            clearBindings();
            super.clear();
        }

        // Bulk operations in AbstractCollection ultimately delegate to
        // add/remove
        // which are guarded above. No need to override
        // addAll/removeAll/retainAll
        // unless optimization is required.

        /**
         * Clears all signal bindings.
         */
        public void clearBindings() {
            getSignalBindingFeatureIfInitialized().ifPresent(feature -> feature
                    .clearBindings(SignalBindingFeature.CLASSES));
        }

        private void throwIfBound(String className) {
            getSignalBindingFeatureIfInitialized().ifPresent(feature -> {
                if (feature
                        .hasBinding(SignalBindingFeature.CLASSES + className)) {
                    throw new BindingActiveException("Class name '" + className
                            + "' is bound and cannot be modified manually");
                }
            });
        }

        private Optional<SignalBindingFeature> getSignalBindingFeatureIfInitialized() {
            try {
                return getNode()
                        .getFeatureIfInitialized(SignalBindingFeature.class);
            } catch (IllegalStateException e) {
                return Optional.empty();
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

}
