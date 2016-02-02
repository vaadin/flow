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
package com.vaadin.client.hummingbird;

import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.reactive.ReactiveChangeEvent;

/**
 * Event fired when the structure of a {@link ListNamespace} changes.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ListSpliceEvent extends ReactiveChangeEvent {

    private int index;
    private JsArray<Object> remove;
    private Object[] add;

    /**
     * Creates a new list splice event.
     *
     * @param source
     *            the changed list namespace
     * @param index
     *            the start index of the changes
     * @param remove
     *            the removed items, not <code>null</code>
     * @param add
     *            the added items, not <code>null</code>
     */
    public ListSpliceEvent(ListNamespace source, int index,
            JsArray<Object> remove, Object[] add) {
        super(source);

        assert remove != null;
        assert add != null;

        this.index = index;
        this.remove = remove;
        this.add = add;
    }

    @Override
    public ListNamespace getSource() {
        return (ListNamespace) super.getSource();
    }

    /**
     * Gets the start index of the changes.
     *
     * @return the start index of the changes
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets an array of removed items.
     *
     * @return array of removed items, not <code>null</code>
     */
    public JsArray<Object> getRemove() {
        return remove;
    }

    /**
     * Gets an array of added items.
     *
     * @return array of added items, not <code>null</code>
     */
    public Object[] getAdd() {
        return add;
    }
}
