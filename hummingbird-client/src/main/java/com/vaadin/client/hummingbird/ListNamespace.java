/*
 * Copyright 2000-2014 Vaadin Ltd.
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
package com.vaadin.client.hummingbird;

import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;

public class ListNamespace extends AbstractNamespace {

    private final JsArray<Object> values = JsCollections.array();

    public ListNamespace(int id, StateNode node) {
        super(id, node);
    }

    public int length() {
        return values.length();
    }

    public Object get(int index) {
        return values.get(index);
    }

    public void set(int index, Object value) {
        values.set(index, value);
    }

    public void splice(int index, int remove) {
        values.splice(index, remove);
    }

    @SafeVarargs
    public final <T> void splice(int index, int remove, T... add) {
        values.splice(index, remove, add);
    }
}
