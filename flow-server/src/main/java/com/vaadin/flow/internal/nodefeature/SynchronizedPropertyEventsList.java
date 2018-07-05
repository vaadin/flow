/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.internal.nodefeature;

import java.util.Set;

import com.vaadin.flow.internal.StateNode;

/**
 * List of synchronized property events for an element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class SynchronizedPropertyEventsList
        extends SerializableNodeList<String> {

    private static class EventsSetView extends NodeList.SetView<String> {

        private EventsSetView(SynchronizedPropertyEventsList list) {
            super(list);
        }

        @Override
        protected void validate(String item) {
            if (item == null) {
                throw new IllegalArgumentException("Event name cannot be null");
            }
        }

    }

    /**
     * Creates a new synchronized property events list for the given node.
     *
     * @param node
     *            the node that the list belongs to
     */
    public SynchronizedPropertyEventsList(StateNode node) {
        super(node);
    }

    /**
     * Creates a view into this list.
     *
     * @return a view into this list
     */
    public Set<String> getSynchronizedPropertyEvents() {
        return new EventsSetView(this);
    }

}
