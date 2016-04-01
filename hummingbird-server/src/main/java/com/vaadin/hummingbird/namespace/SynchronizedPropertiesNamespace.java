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
package com.vaadin.hummingbird.namespace;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.util.JsonUtil;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonValue;

/**
 * Map for storing the data related to property synchronization from the client
 * side to the server.
 *
 * @author Vaadin
 * @since
 */
public class SynchronizedPropertiesNamespace extends MapNamespace {

    public static final String KEY_EVENTS = "events";
    public static final String KEY_PROPERTIES = "properties";

    private static class SetView extends AbstractSet<String>
            implements Serializable {

        private final SynchronizedPropertiesNamespace namespace;
        private final String key;

        private transient int serial;

        private SetView(SynchronizedPropertiesNamespace namespace, String key) {
            this.namespace = namespace;
            this.key = key;
        }

        @Override
        public Iterator<String> iterator() {
            return new SetViewIterator(this, serial);
        }

        @Override
        public boolean add(String item) {
            return addAll(Collections.singleton(item));
        }

        @Override
        public boolean addAll(Collection<? extends String> items) {
            Set<String> set;
            if (namespace.get(key) == null) {
                set = new HashSet<>();
            } else {
                set = getDataSet();
            }
            int size = set.size();
            set.addAll(items);
            if (size != set.size()) {
                namespace.putJson(key, set.stream().map(Json::create)
                        .collect(JsonUtil.asArray()));
                serial++;
                return true;
            }
            return false;
        }

        @Override
        public boolean remove(Object item) {
            return removeAll(Collections.singleton(item));
        }

        @Override
        public void clear() {
            namespace.putJson(key, JsonUtil.createArray());
            serial++;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            if (isEmpty()) {
                return false;
            }
            Set<String> set = getDataSet();
            int size = set.size();
            set.removeAll(c);
            if (size != set.size()) {
                namespace.putJson(key, set.stream().map(Json::create)
                        .collect(JsonUtil.asArray()));
                serial++;
                return true;
            }
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            if (isEmpty()) {
                return false;
            }
            return getDataSet().containsAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            if (isEmpty()) {
                return false;
            }
            Set<String> set = getDataSet();
            int size = set.size();
            set.retainAll(c);
            if (size != set.size()) {
                namespace.putJson(key, set.stream().map(Json::create)
                        .collect(JsonUtil.asArray()));
                serial++;
                return true;
            }
            return false;
        }

        @Override
        public boolean contains(Object o) {
            if (isEmpty()) {
                return false;
            }
            return getDataSet().contains(o);
        }

        @Override
        public int size() {
            JsonArray array = getDataArray();
            if (array == null) {
                return 0;
            }
            return array.length();
        }

        private JsonArray getDataArray() {
            return (JsonArray) namespace.get(key);
        }

        private Set<String> getDataSet() {
            return JsonUtil.stream(getDataArray()).map(JsonValue::asString)
                    .collect(Collectors.toSet());
        }

    }

    private static class SetViewIterator
            implements Iterator<String>, Serializable {

        private final SetView view;
        private final Iterator<String> iterator;

        private final int serial;

        private SetViewIterator(SetView view, int serial) {
            this.view = view;
            iterator = view.isEmpty()
                    ? Collections.<String> emptyList().iterator()
                    : view.getDataSet().iterator();
            this.serial = serial;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public String next() {
            if (serial != view.serial) {
                throw new ConcurrentModificationException();
            }
            return iterator.next();
        }

    }

    /**
     * Creates a new namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     */
    public SynchronizedPropertiesNamespace(StateNode node) {
        super(node);
    }

    /**
     * Gets the names of the properties to synchronize from the client to the
     * server.
     *
     * @return the names of the properties to synchronize
     */
    public Set<String> getSynchronizedProperties() {
        return new SetView(this, KEY_PROPERTIES);
    }

    /**
     * Gets the event types which should trigger synchronization of properties
     * from the client side to the server.
     *
     * @return the event types which should trigger synchronization
     */
    public Set<String> getSynchronizedPropertyEvents() {
        return new SetView(this, KEY_EVENTS);
    }

}
