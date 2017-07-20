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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.ui.Component;

/**
 * @author Vaadin Ltd.
 */
class CustomElements {
    private final Map<String, Set<Class<? extends Component>>> elements = new HashMap<>();

    Map<String, Class<? extends Component>> getElements() {
        return elements.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().iterator().next()));
    }

    void put(String elementName, Class<? extends Component> newClass) {
        elements.put(elementName,
                elements.computeIfAbsent(elementName,
                        key -> Collections.singleton(newClass)).stream()
                        .map(oldClass -> pickTheOneToLeave(oldClass, newClass))
                        .collect(Collectors.toSet()));
    }

    private static Class<? extends Component> pickTheOneToLeave(
            Class<? extends Component> oldClass,
            Class<? extends Component> newClass) {
        if (Objects.equals(oldClass, newClass)
                || newClass.isAssignableFrom(oldClass)) {
            return newClass;
        }
        if (!oldClass.isAssignableFrom(newClass)) {
            String msg = String.format(
                    "Components '%s' and '%s' have the same @Tag annotation, but neither is a super class of the other.",
                    oldClass.getCanonicalName(), newClass.getCanonicalName());
            throw new ClassCastException(msg);
        }
        return oldClass;
    }

}
