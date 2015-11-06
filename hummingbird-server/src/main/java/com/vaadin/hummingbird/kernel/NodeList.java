package com.vaadin.hummingbird.kernel;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.function.Consumer;

import com.vaadin.hummingbird.kernel.change.ListInsertChange;
import com.vaadin.hummingbird.kernel.change.ListRemoveChange;
import com.vaadin.hummingbird.kernel.change.ListReplaceChange;

public class NodeList extends AbstractList<Object> implements Serializable {
    private Object key;
    private ArrayList<Object> backing;
    private StateNode node;

    public NodeList(StateNode node, Object key, ArrayList<Object> backing) {
        assert key != null;
        assert node != null;
        assert backing != null;
        this.node = node;
        this.key = key;
        this.backing = backing;
    }

    @Override
    public Object set(int index, Object element) {
        ensureAttached();

        Object previous = backing.set(index, element);
        node.logChange(new ListReplaceChange(index, key, previous, element));
        node.detach(previous);
        node.attach(element);
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
        node.attach(element);
    }

    @Override
    public Object remove(int index) {
        ensureAttached();
        Object removed = backing.remove(index);
        node.logChange(new ListRemoveChange(index, key, removed));
        node.detach(removed);

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
        backing.forEach(node::detach);
        backing = null;
    }

    void forEachChildNode(Consumer<Object> consumer) {
        backing.forEach(consumer);
    }

    void rollback(ListInsertChange change) {
        backing.remove(change.getIndex());
    }

    public void rollback(ListRemoveChange change) {
        backing.add(change.getIndex(), change.getValue());
    }

    public void rollback(ListReplaceChange change) {
        backing.set(change.getIndex(), change.getOldValue());
    }
}
