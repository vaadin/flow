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
package com.vaadin.flow.dom.impl;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementClassList;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.Signal;
import com.vaadin.signals.ValueSignal;

/**
 * Emulates the <code>class</code> attribute by delegating to
 * {@link Element#getClassList()}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.0
 */
public class ClassAttributeHandler extends CustomAttribute {

    @Override
    public boolean hasAttribute(Element element) {
        return !element.getClassList().isEmpty();
    }

    @Override
    public String getAttribute(Element element) {
        if (element.getNode().isAttached() && element.getNode()
                .getFeature(ElementClassList.class).getSignal() != null) {
            return ((ValueSignal<String>) element.getNode()
                    .getFeature(ElementClassList.class).getSignal()).peek();
        }
        Set<String> classList = element.getClassList();
        if (classList.isEmpty()) {
            return null;
        } else {
            return classList.stream().collect(Collectors.joining(" "));
        }
    }

    @Override
    public void setAttribute(Element element, String value,
            boolean ignoreSignal) {
        if (!ignoreSignal && element.getNode().isAttached() && element.getNode()
                .getFeature(ElementClassList.class).getSignal() != null) {
            throw new BindingActiveException(
                    "setAttribute is not allowed while binding is active.");
        }

        if (!ignoreSignal) {
            ElementClassList list = element.getNode()
                    .getFeature(ElementClassList.class);
            if (list.getSignal() != null) {
                // remove any existing binding
                list.bindSignal(null, null);
            }
        }

        // Disable signal removal temporarily to avoid unintentional removals
        // via internal modifications in the class list.
        element.getNode().getFeature(ElementClassList.class)
                .setSignalRemovalEnabled(false);
        try {
            Set<String> classList = element.getClassList();
            classList.clear();

            String classValue = value.trim();

            if (classValue.isEmpty()) {
                return;
            }

            String[] parts = classValue.split("\\s+");
            classList.addAll(Arrays.asList(parts));
        } finally {
            element.getNode().getFeature(ElementClassList.class)
                    .setSignalRemovalEnabled(true);
        }
    }

    @Override
    public void bindSignal(StateNode node, Signal<String> signal,
            SerializableSupplier<Registration> bindAction) {
        node.getFeature(ElementClassList.class).bindSignal(signal, bindAction);
    }

    @Override
    public void removeAttribute(Element element) {
        if (element.getNode().isAttached() && element.getNode()
                .getFeature(ElementClassList.class).getSignal() != null) {
            throw new BindingActiveException(
                    "removeAttribute is not allowed while binding is active.");
        }
        element.getClassList().clear();
    }
}
