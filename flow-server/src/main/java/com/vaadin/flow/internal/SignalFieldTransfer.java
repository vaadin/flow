/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Transfers local signal values from an old view instance to a new view
 * instance during hotswap refresh.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class SignalFieldTransfer implements Serializable {

    /**
     * Transfers local signal field values from the old instance to the new
     * instance by matching fields by name and type.
     *
     * @param oldInstance
     *            the old view instance being replaced
     * @param newInstance
     *            the new view instance being created
     */
    public static void transferLocalSignalValues(HasElement oldInstance,
            HasElement newInstance) {
        Class<?> newClass = newInstance.getClass();
        while (newClass != null && newClass != Object.class) {
            for (Field newField : newClass.getDeclaredFields()) {
                if (Modifier.isStatic(newField.getModifiers())) {
                    continue;
                }
                if (!isLocalSignalType(newField.getType())) {
                    continue;
                }
                try {
                    transferField(oldInstance, newInstance, newField);
                } catch (Exception e) {
                    getLogger().debug(
                            "Failed to transfer signal field '{}': {}",
                            newField.getName(), e.getMessage());
                }
            }
            newClass = newClass.getSuperclass();
        }
    }

    private static boolean isLocalSignalType(Class<?> type) {
        return ValueSignal.class.isAssignableFrom(type)
                || ListSignal.class.isAssignableFrom(type);
    }

    private static void transferField(HasElement oldInstance,
            HasElement newInstance, Field newField) throws Exception {
        Field oldField = findField(oldInstance.getClass(), newField.getName());
        if (oldField == null) {
            return;
        }
        if (!isLocalSignalType(oldField.getType())) {
            return;
        }

        newField.setAccessible(true);
        oldField.setAccessible(true);

        Object oldSignal = oldField.get(oldInstance);
        Object newSignal = newField.get(newInstance);
        if (oldSignal == null || newSignal == null) {
            return;
        }

        if (oldSignal instanceof ValueSignal<?> oldValue
                && newSignal instanceof ValueSignal<?> newValue) {
            transferValueSignal(oldValue, newValue);
        } else if (oldSignal instanceof ListSignal<?> oldList
                && newSignal instanceof ListSignal<?> newList) {
            transferListSignal(oldList, newList);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void transferValueSignal(ValueSignal<?> oldSignal,
            ValueSignal<?> newSignal) {
        ((ValueSignal) newSignal).set(oldSignal.peek());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void transferListSignal(ListSignal<?> oldSignal,
            ListSignal<?> newSignal) {
        List<ValueSignal<?>> oldEntries = (List) oldSignal.peek();
        newSignal.clear();
        for (ValueSignal<?> entry : oldEntries) {
            ((ListSignal) newSignal).insertLast(entry.peek());
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(SignalFieldTransfer.class);
    }
}
