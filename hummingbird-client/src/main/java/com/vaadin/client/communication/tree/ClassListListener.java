package com.vaadin.client.communication.tree;

import com.google.gwt.dom.client.Element;
import com.vaadin.client.JsArrayObject;
import com.vaadin.client.communication.DomApi;
import com.vaadin.client.communication.tree.ListTreeNode.ArrayEventListener;

public class ClassListListener implements ArrayEventListener {

    private final Element element;

    public ClassListListener(Element element) {
        this.element = element;
    }

    @Override
    public void splice(ListTreeNode listTreeNode, int startIndex,
            JsArrayObject<Object> removed, JsArrayObject<Object> added) {
        for (int i = 0; i < removed.size(); i++) {
            String className = (String) removed.get(i);
            DomApi.wrap(element).getClassList().remove(className);
            TreeUpdater.debug("Removed class: " + className);
        }
        for (int i = 0; i < added.size(); i++) {
            String className = (String) added.get(i);
            DomApi.wrap(element).getClassList().add(className);
            TreeUpdater.debug("Added class: " + className);
        }
    }
}
