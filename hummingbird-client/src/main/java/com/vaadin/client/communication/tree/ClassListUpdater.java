package com.vaadin.client.communication.tree;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.vaadin.client.JsArrayObject;
import com.vaadin.client.communication.DomApi;
import com.vaadin.client.communication.PolymerDomApiImpl;

public class ClassListUpdater {

    private static JsArray<Element> updatedElements = JsArray.createArray()
            .cast();

    public static void splice(Element element, ListTreeNode listTreeNode,
            int startIndex, JsArrayObject<Object> removed,
            JsArrayObject<Object> added) {
        for (int i = 0; i < removed.size(); i++) {
            String className = (String) removed.get(i);
            DomApi.wrap(element).getClassList().remove(className);
            TreeUpdater.debug("Removed class: " + className + " from "
                    + TreeUpdater.debugHtml(element));
        }
        for (int i = 0; i < added.size(); i++) {
            String className = (String) added.get(i);
            DomApi.wrap(element).getClassList().add(className);
            TreeUpdater.debug("Added class: " + className + " for "
                    + TreeUpdater.debugHtml(element));
        }
        updatedElements.push(element);
    }

    public static void updateStyles() {
        if (PolymerDomApiImpl.isAvailable()) {
            while (updatedElements.length() > 0) {
                Element e = updatedElements.shift();
                updateStyles(e);
            }
        } else {
            updatedElements.setLength(0);
        }
    }

    public static native void updateStyles(Element e)
    /*-{
        if (e.updateStyles) {
            e.updateStyles();
        }
     }-*/;
}
