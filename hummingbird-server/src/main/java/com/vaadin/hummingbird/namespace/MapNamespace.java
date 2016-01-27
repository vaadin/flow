package com.vaadin.hummingbird.namespace;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.change.MapPutChange;
import com.vaadin.hummingbird.change.MapRemoveChange;
import com.vaadin.hummingbird.change.NodeChange;

public abstract class MapNamespace extends Namespace {
    private static final Object REMOVED_MARKER = new Object();

    private Map<String, Object> values = new HashMap<>();

    private Map<String, Object> changes = new HashMap<>();

    public MapNamespace(StateNode node) {
        super(node);
    }

    protected void put(String key, Object value) {
        attachPotentialChild(value);

        setChanged(key);
        Object oldValue = values.put(key, value);

        detatchPotentialChild(oldValue);
    }

    protected Object get(String key) {
        setAccessed(key);
        return values.get(key);
    }

    protected boolean contains(String key) {
        setAccessed(key);
        return values.containsKey(key);
    }

    protected void remove(String key) {
        setChanged(key);
        Object oldValue = values.remove(key);
        detatchPotentialChild(oldValue);
    }

    private void setChanged(String key) {
        assert key != null;

        getNode().markAsDirty();

        if (!changes.containsKey(key)) {
            // Record this as changed for the collection logic
            if (values.containsKey(key)) {
                Object oldValue = values.get(key);
                changes.put(key, oldValue);
            } else {
                changes.put(key, REMOVED_MARKER);
            }
        }

        // TODO notify listeners
    }

    private void setAccessed(String key) {
        assert key != null;

        // TODO register listener
    }

    @Override
    public void collectChanges(Consumer<NodeChange> collector) {
        changes.forEach((key, earlierValue) -> {
            boolean containsNow = values.containsKey(key);
            boolean containedEarlier = earlierValue != REMOVED_MARKER;
            if (containedEarlier && !containsNow) {
                collector.accept(new MapRemoveChange(this, key));
            } else if (containsNow) {
                Object currentValue = values.get(key);
                if (!containedEarlier
                        || !Objects.equals(earlierValue, currentValue)) {
                    // New or changed value
                    collector.accept(new MapPutChange(this, key, currentValue));
                }
            }
        });
        changes.clear();
    }

    @Override
    public void resetChanges() {
        changes.clear();
        values.keySet().forEach(k -> changes.put(k, REMOVED_MARKER));
    }
}
