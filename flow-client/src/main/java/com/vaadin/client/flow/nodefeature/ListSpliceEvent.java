/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.client.flow.nodefeature;

import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.reactive.ReactiveValueChangeEvent;

/**
 * Event fired when the structure of a {@link NodeList} changes.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ListSpliceEvent extends ReactiveValueChangeEvent {

    private int index;
    private JsArray<?> remove;
    private JsArray<?> add;
    private boolean clear;

    /**
     * Creates a new list splice event.
     *
     * @param source
     *            the changed list
     * @param index
     *            the start index of the changes
     * @param remove
     *            the removed items, not <code>null</code>
     * @param add
     *            the added items, not <code>null</code>
     * @param clear
     *            <code>true</code> when this is an event triggered upon
     *            removing all the nodes of the given list, <code>false</code>
     *            otherwise
     */
    public ListSpliceEvent(NodeList source, int index, JsArray<?> remove,
            JsArray<?> add, boolean clear) {
        super(source);

        assert remove != null;
        assert add != null;

        this.index = index;
        this.remove = remove;
        this.add = add;
        this.clear = clear;
    }

    @Override
    public NodeList getSource() {
        return (NodeList) super.getSource();
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
    public JsArray<?> getRemove() {
        return remove;
    }

    /**
     * Gets an array of added items.
     *
     * @return array of added items, not <code>null</code>
     */
    public JsArray<?> getAdd() {
        return add;
    }

    /**
     * Gets whether this event is a {@code clear} event.
     * 
     * @return <code>true</code> if the event was triggered after a full clear,
     *         <code>false</code> otherwise.
     */
    public boolean isClear() {
        return clear;
    }
}
