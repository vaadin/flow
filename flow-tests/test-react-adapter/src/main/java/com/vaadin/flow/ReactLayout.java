/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import java.util.Arrays;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.react.ReactAdapterComponent;
import com.vaadin.flow.dom.Element;

@JsModule("./ReactLayout.tsx")
@Tag("react-layout")
public class ReactLayout extends ReactAdapterComponent {

    public static final String MAIN_CONTENT = "content";
    public static final String SECONDARY_CONTENT = "second";

    public ReactLayout(Component... components) {
        add(components);
    }

    public void add(Component... components) {
        Arrays.stream(components).forEach(this::add);
    }

    public void add(Component components) {
        getContentElement(MAIN_CONTENT).appendChild(components.getElement());
    }

    @Override
    public Stream<Component> getChildren() {
        return getContentElement(MAIN_CONTENT).getChildren()
                .map(Element::getComponent)
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty));
    }

    public void addSecondary(Component... components) {
        for (Component component : components) {
            getContentElement(SECONDARY_CONTENT)
                    .appendChild(component.getElement());
        }
    }

    public Stream<Component> getSecondaryChildren() {
        return getContentElement(SECONDARY_CONTENT).getChildren()
                .map(Element::getComponent)
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty));
    }

}
