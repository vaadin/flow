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
