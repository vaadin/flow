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
import com.vaadin.flow.internal.nodefeature.ElementClassList;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.Signal;

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
        Set<String> classList = element.getClassList();
        if (classList.isEmpty()) {
            return null;
        } else {
            return classList.stream().collect(Collectors.joining(" "));
        }
    }

    @Override
    public void setAttribute(Element element, String value) {
        if (element.getNode().getFeature(ElementClassList.class)
                .getSignal() != null) {
            throw new BindingActiveException(
                    "setAttribute is not allowed while binding is active.");
        }

        ElementClassList list = element.getNode()
                .getFeature(ElementClassList.class);
        if (list.getSignal() != null) {
            // remove any existing binding
            list.bindSignal(null, null);
        }

        Set<String> classList = element.getClassList();
        classList.clear();

        String classValue = value.trim();

        if (classValue.isEmpty()) {
            return;
        }

        String[] parts = classValue.split("\\s+");
        classList.addAll(Arrays.asList(parts));
    }

    @Override
    public void bindSignal(Element owner, Signal<String> signal) {
        owner.getNode().getFeature(ElementClassList.class).bindSignal(owner,
                signal);
    }

    @Override
    public void removeAttribute(Element element) {
        if (element.getNode().getFeature(ElementClassList.class)
                .getSignal() != null) {
            throw new BindingActiveException(
                    "removeAttribute is not allowed while binding is active.");
        }
        element.getClassList().clear();
    }
}
