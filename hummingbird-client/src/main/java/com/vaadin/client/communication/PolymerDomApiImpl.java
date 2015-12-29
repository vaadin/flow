package com.vaadin.client.communication;

import com.google.gwt.dom.client.Node;

public class PolymerDomApiImpl implements DomApiImpl {

    public PolymerDomApiImpl() {
        updateStyles();
    }

    /*
     * In case the initial loading page has contained Polymer style modules,
     * need to trigger update styles for those. Since Polymer.updateStyles can
     * be costly, it should not be triggered (and currently isn't) otherwise.
     */
    protected native void updateStyles()
    /*-{
        $wnd.Polymer.updateStyles();
    }-*/;

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

    public native static boolean isAvailable()
    /*-{
         return $wnd.Polymer && $wnd.Polymer.dom;
     }-*/;
}
