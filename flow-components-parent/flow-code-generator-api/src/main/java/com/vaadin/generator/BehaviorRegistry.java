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
package com.vaadin.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.ui.HasClickListeners;
import com.vaadin.ui.HasText;

/**
 * Registry that maps behaviors and mixins from the client side to interfaces at
 * the server-side. Webcomponents that contain the registered behaviors or
 * mixins will inherit from the respective registered Java interface.
 *
 */
public final class BehaviorRegistry {

    private static final Map<String, List<Class<?>>> REGISTRY = new LinkedHashMap<>();

    static {
        put(HasClickListeners.class, "Polymer.PaperButtonBehavior",
                "Polymer.GestureEventListeners", "vaadin-button");
        put(HasText.class, "vaadin-button", "paper-button");
    }

    private BehaviorRegistry() {
    }

    private static void put(Class<?> clazz, String... behaviors) {
        assert clazz
                .isInterface() : "Only interfaces are allowed in the registry.";
        for (String behavior : behaviors) {
            List<Class<?>> listOfClasses = REGISTRY.getOrDefault(behavior,
                    new ArrayList<>());
            listOfClasses.add(clazz);
            REGISTRY.putIfAbsent(behavior, listOfClasses);
        }
    }

    /**
     * Gets the interfaces to be inherited by the generated Java class, based on
     * the list of behaviors and mixins that the original webcomponent contains.
     * 
     * @param behaviors
     *            Collection of behaviors and mixins extracted from the
     *            webcomponent.
     * @return a Set of interfaces for the generated Java class to inherit from.
     *         It returns an empty Set if there are no interfaces registered for
     *         the given behaviors/mixins.
     */
    public static Set<Class<?>> getClassesForBehaviors(
            Iterable<String> behaviors) {
        Set<Class<?>> setOfClasses = new LinkedHashSet<>();

        if (behaviors != null) {
            for (String behavior : behaviors) {
                setOfClasses.addAll(REGISTRY.getOrDefault(behavior,
                        Collections.emptyList()));
            }
        }

        return setOfClasses;
    }

}
