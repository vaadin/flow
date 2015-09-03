package com.vaadin.client.communication;

import com.google.gwt.dom.client.Node;

public class PolymerDomApiImpl implements DomApiImpl {

    @Override
    public native DomElement wrap(Node element)
    /*-{
    return $wnd.Polymer.dom(element);
    }-*/;

    @Override
    public native void flushDom()
    /*-{
     $wnd.Polymer.dom.flush();
    }-*/;
}
