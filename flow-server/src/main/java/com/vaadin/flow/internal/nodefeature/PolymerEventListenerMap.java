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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.internal.ConstantPoolKey;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.Registration;

import elemental.json.Json;

/**
 * Map of PolymerTemplate events with server-side listeners. The key set of this
 * map describes the event types for which event date is present.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class PolymerEventListenerMap extends NodeMap {
    /*
     * Shared empty serializable set instance to avoid allocating lots of memory
     * for the default case of no event data expressions at all. Cannot easily
     * make the instance immutable while still implementing ArrayList. To avoid
     * accidental modification, we instead assert that it's empty when it's
     * used.
     */
    private static final ArrayList<String> emptyArrayList = new ArrayList<>(0);

    private Map<String, List<String>> typeToExpressions;

    /**
     * Creates a new map feature for the given node.
     *
     * @param node
     *            the node that the feature belongs to
     */
    public PolymerEventListenerMap(StateNode node) {
        super(node);
    }

    /**
     * Adds a listener for a event created from a template method.
     * 
     * @param methodName
     *            the name of the method
     * @param eventDataExpressions
     *            the event data expressions
     * @return handler to remove eventType data
     */
    public Registration add(String methodName,
            String[] eventDataExpressions) {
        assert methodName != null;
        assert eventDataExpressions != null;

        if (typeToExpressions == null) {
            typeToExpressions = new HashMap<>();
        }

        // Could optimize slightly by integrating the initialization into the
        // main logic, but that would make the code much harder to read
        if (!contains(methodName)) {
            // Make sure the "immutable" instance hasn't accidentally been
            // mutated
            assert emptyArrayList.isEmpty();
            typeToExpressions.put(methodName, emptyArrayList);
            put(methodName, createConstantPoolKey(emptyArrayList));
        }

        if (eventDataExpressions.length != 0) {
            List<String> eventData = new ArrayList<>(
                    typeToExpressions.get(methodName));

            if (eventData.addAll(Arrays.asList(eventDataExpressions))) {
                // Update the constant pool reference if the value has changed
                put(methodName, createConstantPoolKey(eventData));

                // Remember value for server-side use
                typeToExpressions.put(methodName, eventData);
            }
        }

        return () -> removeListener(methodName);
    }

    private static ConstantPoolKey createConstantPoolKey(
            List<String> eventData) {
        return new ConstantPoolKey(eventData.stream().map(Json::create)
                .collect(JsonUtils.asArray()));
    }

    private void removeListener(String eventType) {
        typeToExpressions.remove(eventType);
        remove(eventType);
    }

}
