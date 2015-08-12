package com.vaadin.ui;

import java.util.Iterator;

import com.vaadin.hummingbird.kernel.Element;

public class ElementBasedComponentIterator implements Iterator<Component> {

    int index = 0;
    private Element element;

    public ElementBasedComponentIterator(Element element) {
        this.element = element;
    }

    @Override
    public boolean hasNext() {
        return element.getChildCount() > index;
    }

    @Override
    public Component next() {
        return element.getChild(index++).getComponent();
    }

}
