package com.vaadin.hummingbird.kernel;

import java.util.AbstractList;

public class ListNodeAsList extends AbstractList<Object> {

    ListNode listNode;

    public ListNodeAsList(ListNode nodeList) {
        this.listNode = nodeList;
    }

    @Override
    public Object set(int index, Object element) {
        return listNode.set(index, element);
    }

    @Override
    public void add(int index, Object element) {
        listNode.add(index, element);
    }

    @Override
    public Object remove(int index) {
        return listNode.remove(index);
    }

    @Override
    public Object get(int index) {
        return listNode.get(index);
    }

    @Override
    public int size() {
        return listNode.size();
    }

}
