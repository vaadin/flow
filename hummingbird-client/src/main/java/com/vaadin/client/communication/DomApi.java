package com.vaadin.client.communication;

import com.google.gwt.dom.client.Node;

public class DomApi {
    private static DomApiImpl impl = new PolymerDomApiImpl();

    public static DomElement wrap(Node node) {
        return impl.wrap(node);
    }

    public static void flush() {
        impl.flushDom();
    }

}
