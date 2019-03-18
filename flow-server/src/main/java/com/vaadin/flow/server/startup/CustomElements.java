/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.flow.server.startup;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;

/**
 * Stores the data about element
 * name({@link Tag} annotation name value) relation to
 * all unique classes with corresponding annotation.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
class CustomElements implements Serializable {
    private final Map<String, Set<Class<? extends Component>>> elements = new HashMap<>();

    private static Optional<String> validateComponentClasses(String tagName,
            Set<Class<? extends Component>> componentClasses) {
        if (componentClasses.size() > 1) {
            return Optional.of(String.format(
                    "Several components are declared with the same @Tag(\"%s\") annotation: %s. "
                            + "Only components that form a hierarchy are allowed "
                            + "to have the same @Tag annotation. Otherwise it's not possible "
                            + "to do mapping between a tag name and a template "
                            + "class in order to instantiate the template when it's defined "
                            + "inside another template",
                            tagName, componentClasses));
        }
        if (componentClasses.size() < 1) {
            return Optional.of(String.format(
                    "Had received tag with name '%s' and no corresponding classes",
                    tagName));
        }
        return Optional.empty();
    }

    private static Class<? extends Component> getComponentClass(
            Map.Entry<String, Set<Class<? extends Component>>> entry) {
        Set<Class<? extends Component>> componentClasses = entry.getValue();
        validateComponentClasses(entry.getKey(), componentClasses)
        .ifPresent(exceptionMessage -> {
            throw new IllegalStateException(exceptionMessage);
        });

        return componentClasses.iterator().next();
    }

    /**
     * Maps each present custom element tag name to exactly one corresponding
     * class. Throws exception if the operation is not possible.
     *
     * @return custom element tag name to corresponding class map
     * @throws IllegalStateException
     *             if
     */
    Map<String, Class<? extends Component>> computeTagToElementRelation() {
        return elements.entrySet().stream().collect(Collectors
                .toMap(Map.Entry::getKey, CustomElements::getComponentClass));
    }

    /**
     * Adds new custom element tag name to class relation.
     *
     * @param elementName
     *            custom element tag name
     * @param newClass
     *            class with corresponding annotation
     */
    void addElement(String elementName, Class<? extends Component> newClass) {
        elements.put(elementName, elements
                .computeIfAbsent(elementName,
                        key -> Collections.singleton(newClass))
                .stream()
                .map(oldClass -> extractNonRelatedClasses(oldClass, newClass))
                .flatMap(Function.identity()).collect(Collectors.toSet()));
    }

    private static Stream<Class<? extends Component>> extractNonRelatedClasses(
            Class<? extends Component> class1,
            Class<? extends Component> class2) {
        if (class1.isAssignableFrom(class2)) {
            return Stream.of(class1);
        } else if (class2.isAssignableFrom(class1)) {
            return Stream.of(class2);
        } else {
            return Stream.of(class1, class2);
        }
    }
}
