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
package com.vaadin.flow.dom.impl;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.vaadin.flow.dom.ClassList;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.template.angular.BindingValueProvider;
import com.vaadin.flow.template.angular.ElementTemplateNode;
import com.vaadin.flow.util.JavaScriptSemantics;

/**
 * Handles CSS class names for a template element.
 *
 * @author Vaadin Ltd
 */
public class BoundClassList extends AbstractSet<String> implements ClassList {

    private final Set<String> staticClasses;
    private final ElementTemplateNode templateNode;
    private final StateNode node;

    /**
     * Creates a new class list for the given template node using data from the
     * given state node.
     *
     * @param templateNode
     *            the template node
     * @param node
     *            the state node
     */
    public BoundClassList(ElementTemplateNode templateNode, StateNode node) {
        this.templateNode = templateNode;
        this.node = node;

        String[] attributeClasses = templateNode.getAttributeBinding("class")
                .map(binding -> binding.getValue(node, "").split("\\s+"))
                .orElseGet(() -> new String[0]);
        staticClasses = new LinkedHashSet<>(Arrays.asList(attributeClasses));
        // Remove defaults that are always overridden by bindings
        templateNode.getClassNames().forEach(staticClasses::remove);
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof String) {
            return contains((String) o);
        } else {
            return false;
        }
    }

    private boolean contains(String className) {
        Optional<BindingValueProvider> binding = templateNode
                .getClassNameBinding(className);
        if (binding.isPresent()) {
            Object bindingValue = binding.get().getValue(node);
            return JavaScriptSemantics.isTrueish(bindingValue);
        } else {
            return staticClasses.contains(className);
        }
    }

    @Override
    public Stream<String> stream() {
        return Stream.concat(staticClasses.stream(),
                templateNode.getClassNames().filter(this::contains));
    }

    @Override
    public Iterator<String> iterator() {
        return stream().iterator();
    }

    @Override
    public int size() {
        return (int) stream().count();
    }
}
