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
package com.vaadin.router.event;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.dom.Element;
import com.vaadin.ui.common.HasElement;

/**
 * Event handling utilities.
 *
 * @author Vaadin Ltd
 */
public final class EventUtil {

    private EventUtil() {
    }

    /**
     * Collect all Components implementing {@link BeforeNavigationObserver} connected to
     * the given element tree.
     * 
     * @param element
     *            element to search from
     * @return navigation listeners
     */
    public static List<BeforeNavigationObserver> collectBeforeNavigationObservers(
            Element element) {
        return getListenerComponents(flattenChildren(element),
                BeforeNavigationObserver.class).collect(Collectors.toList());
    }


    /**
     * Collect all Components implementing {@link BeforeLeaveObserver} connected to
     * the given element tree.
     *
     * @param element
     *            element to search from
     * @return navigation listeners
     */
    public static List<BeforeLeaveObserver> collectBeforeLeaveObservers(
            Element element) {
        return getListenerComponents(flattenChildren(element),
                BeforeLeaveObserver.class).collect(Collectors.toList());
    }

    /**
     * Collect all Components implementing {@link BeforeNavigationObserver} connected to
     * the tree of all given Components in list.
     *
     * @param components
     *            components to search
     * @return navigation listeners
     */
    public static List<BeforeNavigationObserver> collectBeforeNavigationObservers(
            List<HasElement> components) {
        Stream<Element> elements = components.stream()
                .flatMap(component -> flattenChildren(component.getElement()));

        return getListenerComponents(elements, BeforeNavigationObserver.class)
                .collect(Collectors.toList());
    }

    /**
     * Collect all Components implementing {@link BeforeEnterObserver} connected to
     * the tree of all given Components in list.
     *
     * @param components
     *            components to search
     * @return navigation listeners
     */
    public static List<BeforeEnterObserver> collectBeforeEnterObservers(
            List<HasElement> components) {
        Stream<Element> elements = components.stream()
                .flatMap(component -> flattenChildren(component.getElement()));

        return getListenerComponents(elements, BeforeEnterObserver.class)
                .collect(Collectors.toList());
    }

    /**
     * Collect all Components implementing {@link AfterNavigationObserver} that are
     * found in the trees of given Components.
     *
     * @param components
     *            components to search
     * @return after navigation listeners
     */
    public static List<AfterNavigationObserver> collectAfterNavigationObservers(
            List<HasElement> components) {
        Stream<Element> elements = components.stream()
                .flatMap(component -> flattenChildren(component.getElement()));

        return getListenerComponents(elements, AfterNavigationObserver.class)
                .collect(Collectors.toList());
    }

    /**
     * Collect all children for given node as a Element stream.
     *
     * @param node
     *            start node to collect child elements from
     * @return stream of Elements
     */
    public static Stream<Element> flattenChildren(Element node) {
        return Stream.concat(Stream.of(node),
                node.getChildren().flatMap(EventUtil::flattenChildren));
    }

    /**
     * Collect elements with Component implementing listener of type T.
     *
     * @param elementStream
     *            collected elements
     * @param type
     *            class type to filter by
     * @return stream of components implementing T
     */
    public static <T> Stream<T> getListenerComponents(
            Stream<Element> elementStream, Class<T> type) {
        return elementStream.flatMap(
                o -> o.getComponent().map(Stream::of).orElseGet(Stream::empty))
                .filter(component -> type
                        .isAssignableFrom(component.getClass()))
                .map(component -> (T) component);
    }
}
