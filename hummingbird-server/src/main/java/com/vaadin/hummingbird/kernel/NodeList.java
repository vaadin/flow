package com.vaadin.hummingbird.kernel;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.function.Consumer;

import com.vaadin.hummingbird.kernel.change.ListInsertChange;
import com.vaadin.hummingbird.kernel.change.ListRemoveChange;
import com.vaadin.hummingbird.kernel.change.ListReplaceChange;
import com.vaadin.hummingbird.kernel.change.NodeListChange;

public class NodeList extends AbstractList<Object> implements Serializable {
    private Object key;
    private ArrayList<Object> backing = new ArrayList<>();
    private StateNode node;

    public NodeList(StateNode node, Object key) {
        assert key != null;
        assert node != null;
        this.node = node;
        this.key = key;
    }

    @Override
    public Object set(int index, Object element) {
        ensureAttached();

        Object previous = backing.set(index, element);
        node.logChange(new ListReplaceChange(index, key, previous, element));
        node.detachChild(previous);
        node.attachChild(element);
        return previous;
    }

    private boolean isAttached() {
        return backing != null;
    }

    private void ensureAttached() {
        if (!isAttached()) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void add(int index, Object element) {
        ensureAttached();

        backing.add(index, element);
        node.logChange(new ListInsertChange(index, key, element));
        node.attachChild(element);
    }

    @Override
    public Object remove(int index) {
        ensureAttached();
        Object removed = backing.remove(index);
        node.logChange(new ListRemoveChange(index, key, removed));
        node.detachChild(removed);

        return removed;
    }

    @Override
    public Object get(int index) {
        ensureAttached();

        return backing.get(index);
    }

    @Override
    public int size() {
        if (!isAttached()) {
            return 0;
        }

        return backing.size();
    }

    void detach() {
        backing.forEach(node::detachChild);
        backing = null;
    }

    void forEachChildNode(Consumer<Object> consumer) {
        backing.forEach(consumer);
    }

    void rollback(NodeListChange change) {
        if (change instanceof ListInsertChange) {
            backing.remove(change.getIndex());
        } else if (change instanceof ListRemoveChange) {
            backing.add(change.getIndex(), change.getValue());
        } else if (change instanceof ListReplaceChange) {
            ListReplaceChange replaceChange = (ListReplaceChange) change;
            backing.set(change.getIndex(), replaceChange.getOldValue());
        } else {
            throw new IllegalArgumentException("Unkown change type "
                    + change.getClass().getName() + " passed to rollback");
        }
    }
}
