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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A {@link Properties} instance which holds a snapshot of the properties given
 * to the constructor and rejects all attempts to modify the contents
 * afterwards.
 * <p>
 * Both the mutating methods and the mutating operations of the
 * {@link #keySet()}, {@link #values()} and {@link #entrySet()} views throw an
 * {@link UnsupportedOperationException}. Use
 * {@code new Properties().putAll(properties)} to get a modifiable copy of the
 * contents.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 */
public class UnmodifiableProperties extends Properties {

    /**
     * Creates a read-only copy of the given properties.
     *
     * @param properties
     *            the properties to copy, not {@code null}
     */
    public UnmodifiableProperties(Properties properties) {
        // The super implementation is used on purpose: the overridden put
        // rejects modifications
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            super.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public synchronized Object setProperty(String key, String value) {
        throw modificationNotSupported();
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        throw modificationNotSupported();
    }

    @Override
    public synchronized void putAll(Map<?, ?> properties) {
        throw modificationNotSupported();
    }

    @Override
    public synchronized Object putIfAbsent(Object key, Object value) {
        throw modificationNotSupported();
    }

    @Override
    public synchronized Object remove(Object key) {
        throw modificationNotSupported();
    }

    @Override
    public synchronized boolean remove(Object key, Object value) {
        throw modificationNotSupported();
    }

    @Override
    public synchronized void clear() {
        throw modificationNotSupported();
    }

    @Override
    public synchronized Object replace(Object key, Object value) {
        throw modificationNotSupported();
    }

    @Override
    public synchronized boolean replace(Object key, Object oldValue,
            Object newValue) {
        throw modificationNotSupported();
    }

    @Override
    public synchronized void replaceAll(
            BiFunction<? super Object, ? super Object, ?> function) {
        throw modificationNotSupported();
    }

    @Override
    public synchronized Object compute(Object key,
            BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        throw modificationNotSupported();
    }

    @Override
    public synchronized Object computeIfAbsent(Object key,
            Function<? super Object, ?> mappingFunction) {
        throw modificationNotSupported();
    }

    @Override
    public synchronized Object computeIfPresent(Object key,
            BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        throw modificationNotSupported();
    }

    @Override
    public synchronized Object merge(Object key, Object value,
            BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        throw modificationNotSupported();
    }

    @Override
    public synchronized void load(Reader reader) throws IOException {
        throw modificationNotSupported();
    }

    @Override
    public synchronized void load(InputStream stream) throws IOException {
        throw modificationNotSupported();
    }

    @Override
    public synchronized void loadFromXML(InputStream stream)
            throws IOException {
        throw modificationNotSupported();
    }

    @Override
    public Set<Object> keySet() {
        return Collections.unmodifiableSet(super.keySet());
    }

    @Override
    public Collection<Object> values() {
        return Collections.unmodifiableCollection(super.values());
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        // The entries are copied into immutable ones so that the values cannot
        // be changed through Entry::setValue either
        return super.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue()))
                .collect(Collectors.toUnmodifiableSet());
    }

    private UnsupportedOperationException modificationNotSupported() {
        return new UnsupportedOperationException(
                "These properties are read-only and may not be changed!");
    }
}
