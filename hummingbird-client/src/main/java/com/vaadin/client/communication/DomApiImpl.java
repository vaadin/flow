package com.vaadin.client.communication;

import com.google.gwt.dom.client.Node;

public interface DomApiImpl {

    DomElement wrap(Node element);

    void flushDom();

}
