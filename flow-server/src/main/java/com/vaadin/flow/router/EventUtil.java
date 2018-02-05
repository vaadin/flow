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
package com.vaadin.flow.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.NodeVisitor;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;

/**
 * Event handling utilities.
 *
 * @author Vaadin Ltd
 */
public final class EventUtil {

    private static class DescendantsVisitor implements NodeVisitor {

        private final Collection<Element> collector;

        DescendantsVisitor(Collection<Element> collector) {
            this.collector = collector;
        }

        @Override
        public void visit(ElementType type, Element element) {
            collector.add(element);
        }

        @Override
        public void visit(ShadowRoot root) {
            // Skip shadow root nodes
        }

    }

    private EventUtil() {
    }

    /**
     * Collect all Components implementing {@link BeforeLeaveObserver} connected
     * to the given element tree.
     *
     * @param element
     *            element to search from
     * @return navigation listeners
     */
    public static List<BeforeLeaveObserver> collectBeforeLeaveObservers(
            Element element) {
        return getImplementingComponents(flattenDescendants(element),
                BeforeLeaveObserver.class).collect(Collectors.toList());
    }

    /**
     * A stream of all Components implementing {@link BeforeEnterObserver} connected
     * to the tree of all given Components in list.
     *
     * @param components
     *            components to search
     * @return navigation listeners
     */
    public static Stream<BeforeEnterObserver> collectBeforeEnterObservers(
            List<HasElement> components) {
        Stream<Element> elements = components.stream().flatMap(
                component -> flattenDescendants(component.getElement()));

        return getImplementingComponents(elements, BeforeEnterObserver.class);
    }

    /**
     * A stream of all Components implementing {@link AfterNavigationObserver} that
     * are found in the trees of given Components.
     *
     * @param components
     *            components to search
     * @return after navigation listeners
     */
    public static Stream<AfterNavigationObserver> collectAfterNavigationObservers(
            List<HasElement> components) {
        Stream<Element> elements = components.stream().flatMap(
                component -> flattenDescendants(component.getElement()));

        return getImplementingComponents(elements,
                AfterNavigationObserver.class);
    }

    /**
     * Collect all Components implementing {@link LocaleChangeObserver}
     * connected to the given element tree.
     *
     * @param element
     *            element to search from
     * @return navigation listeners
     */
    public static List<LocaleChangeObserver> collectLocaleChangeObservers(
            Element element) {
        return getImplementingComponents(flattenDescendants(element),
                LocaleChangeObserver.class).collect(Collectors.toList());
    }

    /**
     * Collect all Components implementing {@link LocaleChangeObserver}
     * connected to the tree of all given Components in list.
     *
     * @param components
     *            components to search
     * @return navigation listeners
     */
    public static List<LocaleChangeObserver> collectLocaleChangeObservers(
            List<HasElement> components) {
        Stream<Element> elements = components.stream().flatMap(
                component -> flattenDescendants(component.getElement()));

        return getImplementingComponents(elements, LocaleChangeObserver.class)
                .collect(Collectors.toList());
    }

    /**
     * Inform components connected to the given ui that implement
     * {@link LocaleChangeObserver} about locale change.
     *
     * @param ui
     *            UI for locale change
     */
    public static void informLocaleChangeObservers(UI ui) {
        LocaleChangeEvent localeChangeEvent = new LocaleChangeEvent(ui,
                ui.getLocale());
        collectLocaleChangeObservers(ui.getElement())
                .forEach(observer -> observer.localeChange(localeChangeEvent));
    }

    /**
     * Inform components implementing {@link LocaleChangeObserver} about locale
     * change.
     *
     * @param ui
     *            UI for locale change
     * @param components
     *            components to search
     */
    public static void informLocaleChangeObservers(UI ui,
            List<HasElement> components) {
        LocaleChangeEvent localeChangeEvent = new LocaleChangeEvent(ui,
                ui.getLocale());
        collectLocaleChangeObservers(components)
                .forEach(observer -> observer.localeChange(localeChangeEvent));
    }

    /**
     * Collect elements with Component implementing listener of type T.
     *
     * @param elementStream
     *            collected elements
     * @param type
     *            class type to filter by
     * @param <T>
     *            type that is used in filtering
     * @return stream of components implementing T
     */
    public static <T> Stream<T> getImplementingComponents(
            Stream<Element> elementStream, Class<T> type) {
        return elementStream.flatMap(
                o -> o.getComponent().map(Stream::of).orElseGet(Stream::empty))
                .filter(component -> type
                        .isAssignableFrom(component.getClass()))
                .map(component -> (T) component);
    }

    /**
     * Collect all children for given node as a Element stream.
     *
     * @param node
     *            start node to collect child elements from
     * @param descendants
     *            a collector of descendants to fill
     */
    public static void inspectHierarchy(Element node,
            Collection<Element> descendants) {
        node.accept(new DescendantsVisitor(descendants), true);
    }

    private static Stream<Element> flattenDescendants(Element element) {
        Collection<Element> descendants = new ArrayList<>();
        inspectHierarchy(element, descendants);
        return descendants.stream();
    }
}
