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

package com.vaadin.client.flow.collection.jre;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections.ForEachCallback;

/**
 * JRE implementation of {@link JsArray}, should only be used for testing.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <T>
 *            item type
 * @deprecated Only to be used for testing
 */
@Deprecated
public class JreJsArray<T> extends JsArray<T> {
    private final List<T> values;

    /**
     * Creates a new array with the given values.
     *
     * @param values
     *            the values of the new array
     */
    @SafeVarargs
    public JreJsArray(T... values) {
        this(Arrays.asList(values));
    }

    private JreJsArray(List<T> values) {
        this.values = new ArrayList<>(values);
    }

    /**
     * JRE implementation of the final {@link #get(int)} method.
     *
     * @param index
     *            the index
     * @return the value at the index
     */
    public T doGet(int index) {
        if (index >= values.size()) {
            return null;
        }
        return values.get(index);
    }

    /**
     * JRE implementation of the final {@link #set(int, Object)} method.
     *
     * @param index
     *            the index to set
     * @param value
     *            the value to set
     */
    public void doSet(int index, T value) {
        while (index >= values.size()) {
            // Setting outside the current range should extend the array as it
            // does in JS
            values.add(null);
        }
        values.set(index, value);
    }

    @Override
    public int push(@SuppressWarnings("unchecked") T... values) {
        this.values.addAll(Arrays.asList(values));
        return this.values.size();
    }

    @Override
    public int length() {
        return values.size();
    }

    /**
     * JRE implementation of the final {@link #spliceArray(int, int, JsArray)}
     * method.
     *
     * @param index
     *            the index at which to do the operation
     * @param remove
     *            the number of items to remove
     * @param add
     *            new items to add
     * @return an array of removed items
     */
    public JsArray<T> doSpliceArray(int index, int remove,
            JsArray<? extends T> add) {
        return doSplice(index, remove, ((JreJsArray<? extends T>) add).values);
    }

    @Override
    public JsArray<T> splice(int index, int remove,
            @SuppressWarnings("unchecked") T... add) {
        return doSplice(index, remove, Arrays.asList(add));
    }

    private JreJsArray<T> doSplice(int index, int remove,
            List<? extends T> add) {
        JreJsArray<T> removed;
        if (remove > 0) {
            List<T> removeRange = values.subList(index, index + remove);
            removed = new JreJsArray<>(removeRange);

            removeRange.clear();
        } else {
            removed = new JreJsArray<>();
        }

        values.addAll(index, add);

        return removed;
    }

    /**
     * Internal method called to clear the array.
     */
    public void doClear() {
        values.clear();
    }

    /**
     * Gets the values of this array as a list. This method is only exposed to
     * simplify testing.
     *
     * @param array
     *            the js array to get the values from
     * @param <T>
     *            the array type
     *
     * @return the values as a list
     */
    public static <T> List<T> asList(JsArray<T> array) {
        return ((JreJsArray<T>) array).values;
    }

    @Override
    public T shift() {
        return isEmpty() ? null : remove(0);
    }

    @Override
    public void forEach(ForEachCallback<T> callback) {
        for (T value : values) {
            callback.accept(value);
        }
    }

}
