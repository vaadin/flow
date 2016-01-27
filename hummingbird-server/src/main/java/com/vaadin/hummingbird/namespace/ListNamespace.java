package com.vaadin.hummingbird.namespace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.change.ListSpliceChange;
import com.vaadin.hummingbird.change.NodeChange;

public abstract class ListNamespace<T> extends Namespace {

    private List<T> values = new ArrayList<>();

    private List<ListSpliceChange> changes = new ArrayList<>();

    private boolean nodeValues;

    public ListNamespace(StateNode node, boolean nodeValues) {
        super(node);
        this.nodeValues = nodeValues;
    }

    public int size() {
        setAccessed();
        return values.size();
    }

    protected T get(int index) {
        setAccessed();
        return values.get(index);
    }

    protected void add(T value) {
        add(values.size(), value);
    }

    protected void add(int index, T value) {
        assert value == null || (value instanceof StateNode == nodeValues);

        if (nodeValues) {
            attachPotentialChild(value);
        }
        values.add(index, value);

        addChange(new ListSpliceChange(this, index, 0,
                Collections.singletonList(value)));
    }

    public void remove(int index) {
        Object removed = values.remove(index);
        detatchPotentialChild(removed);

        addChange(
                new ListSpliceChange(this, index, 1, Collections.emptyList()));
    }

    private void addChange(ListSpliceChange change) {
        getNode().markAsDirty();

        // XXX combine with previous changes if possible
        changes.add(change);

        // TODO Fire some listeners
    }

    private void setAccessed() {
        // TODO Set up listener if we're in a computation
    }

    @Override
    public void collectChanges(Consumer<NodeChange> collector) {
        changes.forEach(collector);
        changes.clear();
    }

    @Override
    public void resetChanges() {
        changes.clear();
        if (!values.isEmpty()) {
            changes.add(
                    new ListSpliceChange(this, 0, 0, new ArrayList<>(values)));
        }
    }

    public boolean isNodeValues() {
        return nodeValues;
    }
}
