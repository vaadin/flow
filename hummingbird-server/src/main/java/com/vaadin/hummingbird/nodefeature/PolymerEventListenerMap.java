package com.vaadin.hummingbird.nodefeature;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.vaadin.hummingbird.ConstantPoolKey;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.hummingbird.util.JsonUtils;

import elemental.json.Json;

public class PolymerEventListenerMap extends NodeMap {
    /*
     * Shared empty serializable set instance to avoid allocating lots of memory
     * for the default case of no event data expressions at all. Cannot easily
     * make the instance immutable while still implementing HashSet. To avoid
     * accidental modification, we instead assert that it's empty when it's
     * used.
     */
    private static final HashSet<String> emptyHashSet = new HashSet<>();

    private HashMap<String, Set<String>> typeToExpressions;

    /**
     * Creates a new map feature for the given node.
     *
     * @param node
     *            the node that the feature belongs to
     */
    public PolymerEventListenerMap(StateNode node) {
        super(node);
    }

    public EventRegistrationHandle add(String eventType,
            String[] eventDataExpressions) {
        assert eventType != null;
        assert eventDataExpressions != null;

        if (typeToExpressions == null) {
            typeToExpressions = new HashMap<>();
        }

        // Could optimize slightly by integrating the initialization into the
        // main logic, but that would make the code much harder to read
        if (!contains(eventType)) {
            // Make sure the "immutable" instance hasn't accidentally been
            // mutated
            assert emptyHashSet.isEmpty();
            typeToExpressions.put(eventType, emptyHashSet);
            put(eventType, createConstantPoolKey(emptyHashSet));
        }

        if (eventDataExpressions.length != 0) {
            HashSet<String> eventData = new HashSet<>(
                    typeToExpressions.get(eventType));

            if (eventData.addAll(Arrays.asList(eventDataExpressions))) {
                // Update the constant pool reference if the value has changed
                put(eventType, createConstantPoolKey(eventData));

                // Remember value for server-side use
                typeToExpressions.put(eventType, eventData);
            }
        }

        return () -> removeListener(eventType);
    }

    private static ConstantPoolKey createConstantPoolKey(
            HashSet<String> eventData) {
        return new ConstantPoolKey(eventData.stream().map(Json::create)
                .collect(JsonUtils.asArray()));
    }

    private void removeListener(String eventType) {
        typeToExpressions.remove(eventType);
        remove(eventType);
    }

}
