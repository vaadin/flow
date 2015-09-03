package com.vaadin.client.communication;

import com.google.gwt.dom.client.Node;

public class PlainDomApiImpl implements DomApiImpl {

    @Override
    public DomElement wrap(Node element) {
        return (DomElement) element;
    }

    @Override
    public void flushDom() {
        // nop
    }

}
