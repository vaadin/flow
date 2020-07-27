/*
 * Copyright 2000-2020 Vaadin Ltd.
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

package com.vaadin.flow.server.connect;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A helper class representing the cache for storing already 
 * visited Bean values. 
 */
class BeanValueTypeCheckHelper {
    private Map<Type, Set<Object>> cache;

    /**
     * Check if the value and type have been visisted.
     */
    boolean hasVisited(Object value, Type type) {
        if (cache == null) {
            return false;
        }
        Set<Object> values = cache.get(type);
        if (values == null) {
            return false;
        }
        return values.contains(value);
    }

    /**
     * Mark the given value and type have been visited.
     */
    void markAsVisited(Object value, Type type) {
        if (cache == null) {
            cache = new HashMap<>();
        }
        cache.putIfAbsent(type, new HashSet<>());
        cache.get(type).add(value);
    }
}
