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

package com.vaadin.client.hummingbird.collection.jre;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.client.hummingbird.collection.JsArray;

/**
 * JRE implementation of {@link JsArray}, should only be used for testing.
 *
 * @since
 * @author Vaadin Ltd
 * @param <T>
 *            item type
 * @deprecated Only to be used for testing
 */
@Deprecated
public class JreJsArray<T> extends JsArray<T> {
    private List<T> values = new ArrayList<>();

    // Special name since actual method must be final
    public T doGet(int index) {
        return values.get(index);
    }

    // Special name since the actual method must be final
    public void doSet(int index, T value) {
        values.set(index, value);
    }

    @Override
    public int push(T value) {
        values.add(value);
        return values.size();
    }

    @Override
    public int length() {
        return values.size();
    }
}
