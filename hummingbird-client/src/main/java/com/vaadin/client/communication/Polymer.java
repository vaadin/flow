package com.vaadin.client.communication;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.vaadin.client.JsArrayObject;

public class Polymer {

    // See https://www.polymer-project.org/1.0/docs/devguide/local-dom.html

    public static native void addClassName(Element element, String className)
    /*-{
    $wnd.Polymer.dom(element).classList.add(className);
    }-*/;

    public static native void removeClassName(Element element, String className)
    /*-{
    $wnd.Polymer.dom(element).classList.remove(className);
    }-*/;

    /* Child manipulation */
    public static native void appendChild(Element parent, Node childNode)
    /*-{
         $wnd.Polymer.dom(parent).appendChild(childNode);
    }-*/;

    public static native JsArrayObject<Node> getChildNodes(Element parent)
    /*-{
    return $wnd.Polymer.dom(parent).childNodes;
    }-*/;

    public static native void removeChild(Element parent, Node childNode)
    /*-{
    $wnd.Polymer.dom(parent).removeChild(childNode);
    }-*/;

    public static native void insertBefore(Element parent, Node child,
            Node reference)
            /*-{
            $wnd.Polymer.dom(parent).insertBefore(child, reference);
            }-*/;

    public static native void flush(Element parent)
    /*-{
    return $wnd.Polymer.dom(parent).flush();
    }-*/;

    /* Node mutation */
    public static native void setAttribute(Element element, String key,
            String value)
            /*-{
            return $wnd.Polymer.dom(element).setAttribute(key,value);
            }-*/;

    /* Node mutation */
    public static native void removeAttribute(Element element, String key)
    /*-{
    return $wnd.Polymer.dom(element).removeAttribute(key);
    }-*/;

}
