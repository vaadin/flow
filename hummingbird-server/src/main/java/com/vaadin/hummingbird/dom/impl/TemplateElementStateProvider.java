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
package com.vaadin.hummingbird.dom.impl;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.ClassList;
import com.vaadin.hummingbird.dom.DomEventListener;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementStateProvider;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.hummingbird.dom.Style;
import com.vaadin.hummingbird.namespace.TemplateNamespace;
import com.vaadin.hummingbird.template.ElementTemplateNode;
import com.vaadin.hummingbird.template.TemplateNode;
import com.vaadin.ui.Component;

import elemental.json.JsonValue;

/**
 * Handles storing and retrieval of the state information for an element defined
 * in a template node.
 *
 * @since
 * @author Vaadin Ltd
 */
public class TemplateElementStateProvider implements ElementStateProvider {

    private static class ImmutableEmptyStyle implements Style {
        @Override
        public String get(String name) {
            return null;
        }

        @Override
        public Style set(String name, String value) {
            throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
        }

        @Override
        public Style remove(String name) {
            throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
        }

        @Override
        public Style clear() {
            throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
        }

        @Override
        public boolean has(String name) {
            return false;
        }

        @Override
        public Stream<String> getNames() {
            return Stream.empty();
        }
    }

    private static class ImmutableEmptyClassList extends AbstractSet<String>
            implements ClassList {

        @Override
        public boolean add(String e) {
            throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
        }

        @Override
        public boolean set(String className, boolean set) {
            throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
        }

        @Override
        public Iterator<String> iterator() {
            return Collections.<String> emptySet().iterator();
        }

        @Override
        public int size() {
            return 0;
        }

    }

    private static final String CANT_MODIFY_MESSAGE = "Can't modify element defined in a template";
    private ElementTemplateNode templateNode;

    /**
     * Creates a new state provider for the given template definition.
     *
     * @param templateNode
     *            the template definition, not <code>null</code>
     */
    public TemplateElementStateProvider(ElementTemplateNode templateNode) {
        assert templateNode != null;
        this.templateNode = templateNode;
    }

    @Override
    public boolean supports(StateNode node) {
        return node.hasNamespace(TemplateNamespace.class);
    }

    @Override
    public String getTag(StateNode node) {
        return templateNode.getTag();
    }

    @Override
    public void setAttribute(StateNode node, String attribute, String value) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public String getAttribute(StateNode node, String attribute) {
        return null;
    }

    @Override
    public boolean hasAttribute(StateNode node, String attribute) {
        return false;
    }

    @Override
    public void removeAttribute(StateNode node, String attribute) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public Stream<String> getAttributeNames(StateNode node) {
        return Stream.empty();
    }

    @Override
    public Element getParent(StateNode node) {
        // Implementation shared with template text provider
        return getParent(node, templateNode);
    }

    /**
     * Gets the parent element for a state node and template node.
     *
     * @param node
     *            the state node, not <code>null</code>
     * @param templateNode
     *            the template node, not <code>null</code>
     * @return the element, not <code>null</code>
     */
    public static Element getParent(StateNode node, TemplateNode templateNode) {
        assert node != null;
        assert templateNode != null;

        return templateNode.getParent()
                .map(parent -> Element.get(node, parent.getStateProvider()))
                .orElseGet(() -> {
                    StateNode parentNode = node.getParent();
                    if (parentNode == null) {
                        return null;
                    }
                    return Element.get(parentNode);
                });
    }

    @Override
    public int getChildCount(StateNode node) {
        return templateNode.getChildCount();
    }

    @Override
    public Element getChild(StateNode node, int index) {
        // Simple initial implementation since template elements can't yet have
        // non-template children
        TemplateNode childTemplate = templateNode.getChild(index);
        return Element.get(node, childTemplate.getStateProvider());
    }

    @Override
    public void insertChild(StateNode node, int index, Element child) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public void removeChild(StateNode node, int index) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public void removeChild(StateNode node, Element child) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public void removeAllChildren(StateNode node) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public EventRegistrationHandle addEventListener(StateNode node,
            String eventType, DomEventListener listener,
            String[] eventDataExpressions) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public Object getProperty(StateNode node, String name) {
        return templateNode.getPropertyBinding(name)
                .map(binding -> binding.getValue(node, "")).orElse(null);
    }

    @Override
    public void setProperty(StateNode node, String name, Serializable value,
            boolean emitChange) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public void setJsonProperty(StateNode node, String name, JsonValue value) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public void removeProperty(StateNode node, String name) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public boolean hasProperty(StateNode node, String name) {
        return templateNode.getPropertyBinding(name).isPresent();
    }

    @Override
    public Stream<String> getPropertyNames(StateNode node) {
        return templateNode.getPropertyNames();
    }

    @Override
    public boolean isTextNode(StateNode node) {
        return false;
    }

    @Override
    public String getTextContent(StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTextContent(StateNode node, String textContent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassList getClassList(StateNode node) {
        // Should eventually be based on [class.foo]=bar in the template
        return new ImmutableEmptyClassList();
    }

    @Override
    public Style getStyle(StateNode node) {
        // Should eventually be based on [style.foo]=bar in the template
        return new ImmutableEmptyStyle();
    }

    @Override
    public Set<String> getSynchronizedProperties(StateNode node) {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getSynchronizedPropertyEvents(StateNode node) {
        return Collections.emptySet();
    }

    @Override
    public void setComponent(StateNode node, Component component) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public Optional<Component> getComponent(StateNode node) {
        return Optional.empty();
    }

}
