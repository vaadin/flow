package com.vaadin.hummingbird.kernel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Consumer;

import com.vaadin.hummingbird.kernel.change.ListChange;
import com.vaadin.hummingbird.kernel.change.ListInsertChange;
import com.vaadin.hummingbird.kernel.change.ListRemoveChange;
import com.vaadin.hummingbird.kernel.change.ListReplaceChange;
import com.vaadin.hummingbird.kernel.change.NodeChange;

public class ListNode extends MapStateNode implements Serializable {
    private static final Object lengthDependencyKey = new Object();

    private ArrayList<Object> backing = new ArrayList<>();

    public ListNode(ValueType memberType) {
        super(ValueType.get(Collections.emptyMap(), memberType));
    }

    public Object set(int index, Object element) {
        Object previous = backing.set(index, element);
        logChange(new ListReplaceChange(index, previous, element));
        detachChild(previous);
        attachChild(element);
        return previous;
    }

    public void add(int index, Object element) {
        backing.add(index, element);
        logChange(new ListInsertChange(index, element));
        attachChild(element);

        if (hasDependents()) {
            updateDependents(lengthDependencyKey, Reactive::registerWrite);
        }
    }

    public Object remove(int index) {
        Object removed = backing.remove(index);
        logChange(new ListRemoveChange(index, removed));
        detachChild(removed);

        if (hasDependents()) {
            updateDependents(lengthDependencyKey, Reactive::registerWrite);
        }

        return removed;
    }

    public Object get(int index) {
        return backing.get(index);
    }

    public int indexOf(Object o) {
        if (Reactive.inComputation()) {
            updateDependents(lengthDependencyKey, Reactive::registerRead);
        }

        return backing.indexOf(o);
    }

    public int size() {
        if (Reactive.inComputation()) {
            updateDependents(lengthDependencyKey, Reactive::registerRead);
        }

        if (backing == null) {
            return 0;
        }

        return backing.size();

    }

    void detach() {
        backing.forEach(this::detachChild);
        backing = null;
    }

    @Override
    protected void forEachChildNode(Consumer<StateNode> consumer) {
        super.forEachChildNode(consumer);
        Consumer<Object> action = new Consumer<Object>() {
            @Override
            public void accept(Object v) {
                if (v instanceof StateNode) {
                    StateNode childNode = (StateNode) v;
                    consumer.accept(childNode);
                }
            }
        };
        backing.forEach(action);

    }

    @Override
    public void rollback(NodeChange change) {
        if (change instanceof ListChange) {
            ListChange listChange = (ListChange) change;
            if (change instanceof ListInsertChange) {
                backing.remove(listChange.getIndex());
            } else if (change instanceof ListRemoveChange) {
                ListRemoveChange removeChange = (ListRemoveChange) change;
                backing.add(removeChange.getIndex(), removeChange.getValue());
            } else if (change instanceof ListReplaceChange) {
                ListReplaceChange replaceChange = (ListReplaceChange) change;
                backing.set(listChange.getIndex(), replaceChange.getOldValue());
            } else {
                throw new IllegalArgumentException("Unkown change type "
                        + change.getClass().getName() + " passed to rollback");
            }
        } else {
            super.rollback(change);
        }
    }

    @Override
    public String toString() {
        String sup = super.toString();
        sup = sup.substring(0, sup.length() - 1);
        sup += ", backing: " + backing + "]";
        return sup;
    }
}
