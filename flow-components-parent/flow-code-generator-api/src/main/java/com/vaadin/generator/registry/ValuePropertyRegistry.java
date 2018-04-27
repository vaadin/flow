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
package com.vaadin.generator.registry;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry that facilitates mapping which property name will be used when
 * the element should extend AbstractField API.
 *   
 * @author Vaadin ltd
 */
public final class ValuePropertyRegistry {

    private static final Map<String, String> REGISTRY = new HashMap<>();
    static {
        REGISTRY.put("vaadin-checkbox", "checked");
    }

    private ValuePropertyRegistry() {
    }

    /**
     * @param elementIdentifier
     *          tag-name of the element
     * @return
     *          property name used for value changes
     */
    public static String valueName(String elementIdentifier) {
        return REGISTRY.containsKey(elementIdentifier)
                ? REGISTRY.get(elementIdentifier) : "value";
    }
}
