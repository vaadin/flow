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
package com.vaadin.ui;

import java.lang.reflect.Field;
import java.util.EventObject;
import java.util.Map.Entry;

import com.vaadin.hummingbird.dom.DomEvent;
import com.vaadin.hummingbird.event.ComponentEventBus;

public class ComponentEvent extends EventObject {

    private boolean fromClient = false;

    public ComponentEvent(Component source) {
        super(source);
    }

    public void initFromDomEvent(DomEvent domEvent) {
        fromClient = true;
        try {
            for (Entry<String, Field> entry : ComponentEventBus
                    .getEventDataFields(getClass()).entrySet()) {
                String eventDataExpression = entry.getKey();
                Field f = entry.getValue();
                f.setAccessible(true);
                // FIXME Types
                f.set(this, get(domEvent, eventDataExpression, Integer.class));
            }
        } catch (IllegalArgumentException | IllegalAccessException
                | SecurityException e) {
            // FIXME
            throw new RuntimeException(e);
        }
    }

    private static Object get(DomEvent domEvent, String key, Class<?> type) {
        if (type == Integer.class) {
            return (int) domEvent.getEventData().getNumber(key);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported type " + type.getName());
        }
    }

    @Override
    public Component getSource() {
        return (Component) super.getSource();
    };

    public boolean isFromClient() {
        return fromClient;
    }
}
