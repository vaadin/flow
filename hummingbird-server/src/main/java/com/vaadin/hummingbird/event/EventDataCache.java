/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.event;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;

import com.vaadin.ui.ComponentEvent;

public class EventDataCache {

    private HashMap<Class<? extends ComponentEvent>, LinkedHashMap<String, Class<?>>> dataExpressions = new HashMap<>();
    private HashMap<Class<? extends ComponentEvent>, Constructor<?>> eventConstructors = new HashMap<>();

    public Optional<LinkedHashMap<String, Class<?>>> getDataExpressions(
            Class<? extends ComponentEvent> eventType) {
        return Optional.ofNullable(dataExpressions.get(eventType));
    }

    public LinkedHashMap<String, Class<?>> setDataExpressions(
            Class<? extends ComponentEvent> eventType,
            LinkedHashMap<String, Class<?>> expressions) {
        dataExpressions.put(eventType, expressions);
        return expressions;
    }

    @SuppressWarnings("unchecked")
    public <T extends ComponentEvent> Optional<Constructor<T>> getEventConstructor(
            Class<T> eventType) {
        return Optional
                .ofNullable((Constructor<T>) eventConstructors.get(eventType));
    }

    public <T extends ComponentEvent> Constructor<T> setEventConstructor(
            Class<T> eventType, Constructor<T> constructor) {
        eventConstructors.put(eventType, constructor);
        return constructor;
    }

    /**
     * Removes any cached values.
     * <p>
     * Internal method for testing.
     */
    void clear() {
        dataExpressions.clear();
        eventConstructors.clear();
    }
}
