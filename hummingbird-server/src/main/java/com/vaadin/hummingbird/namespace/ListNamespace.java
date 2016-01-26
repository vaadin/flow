package com.vaadin.hummingbird.namespace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.change.ListSpliceChange;
import com.vaadin.hummingbird.change.NodeChange;

public abstract class ListNamespace extends Namespace {

    private List<Object> values = new ArrayList<>();

    private List<ListSpliceChange> changes = new ArrayList<>();

    public ListNamespace(StateNode node) {
        super(node);
    }

    public int size() {
        setAccessed();
        return values.size();
    }

    protected Object get(int index) {
        setAccessed();
        return values.get(index);
    }

    protected void add(Object value) {
        add(values.size(), value);
    }

    protected void add(int index, Object value) {
        values.add(index, value);

        addChange(new ListSpliceChange(this, index, 0,
                Collections.singletonList(value)));
    }

    public void remove(int index) {
        values.remove(index);

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
}
