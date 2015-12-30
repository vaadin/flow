package com.vaadin.client.communication;

import com.google.gwt.dom.client.Node;

public class PolymerDomApiImpl implements DomApiImpl {

    public native static void updateStyles()
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

    public native static boolean isPolymerMicroLoaded()
    /*-{
         return $wnd.Polymer && $wnd.Polymer.dom;
     }-*/;

    public native static boolean isPolymerFullLoaded()
    /*-{
         return $wnd.Polymer && $wnd.Polymer.updateStyles;
     }-*/;
}
