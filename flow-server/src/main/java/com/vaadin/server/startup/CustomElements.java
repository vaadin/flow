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

package com.vaadin.server.startup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.ui.Component;

/**
 * @author Vaadin Ltd.
 */
class CustomElements {
    private final Map<String, Set<Class<? extends Component>>> elements = new HashMap<>();

    Map<String, Class<? extends Component>> getElements() {
        return elements.entrySet().stream().collect(Collectors
                .toMap(Map.Entry::getKey, CustomElements::getComponentClass));
    }

    private static Class<? extends Component> getComponentClass(
            Map.Entry<String, Set<Class<? extends Component>>> entry) {
        Set<Class<? extends Component>> componentClasses = entry.getValue();
        if (componentClasses.size() > 1) {
            String msg = String.format(
                    "Components '%s' have the same @Tag annotation, but neither is a super class of the other.",
                    componentClasses);
            throw new ClassCastException(msg);
        }
        return componentClasses.iterator().next();
    }

    void put(String elementName, Class<? extends Component> newClass) {
        elements.put(elementName, elements
                .computeIfAbsent(
                        elementName, key -> Collections.singleton(newClass))
                .stream()
                .map(oldClass -> newClass.isAssignableFrom(oldClass)
                        ? Stream.of(newClass)
                        : Stream.of(oldClass, newClass))
                .flatMap(Function.identity()).collect(Collectors.toSet()));
    }
}
