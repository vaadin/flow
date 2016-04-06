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

import java.util.Set;

import com.vaadin.hummingbird.StateNode;

/**
 * Namespace for synchronized property events for an element.
 * 
 * @author Vaadin Ltd
 * @since
 */
public class SynchronizedPropertyEventsNamespace
        extends SerializableListNamespace<String> {

    private static class EventsSetView extends ListNamespace.SetView<String> {

        private EventsSetView(SynchronizedPropertyEventsNamespace namespace) {
            super(namespace);
        }

        @Override
        protected void validate(String item) {
            if (item == null) {
                throw new IllegalArgumentException("Event name cannot be null");
            }
        }

    }

    /**
     * Creates a new synchronized property events set namespace for the given
     * node.
     *
     * @param node
     *            the node that the namespace belongs to
     */
    public SynchronizedPropertyEventsNamespace(StateNode node) {
        super(node);
    }

    /**
     * Creates a view into this namespace.
     *
     * @return a view into this namespace
     */
    public Set<String> getSynchronizedPropertyEvents() {
        return new EventsSetView(this);
    }

}
