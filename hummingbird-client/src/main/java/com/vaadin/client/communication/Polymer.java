package com.vaadin.client.communication;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.vaadin.client.JsArrayObject;

public class Polymer {

    public static native PolymerDOM dom(Element element)
    /*-{
    return $wnd.Polymer.dom(element);
    }-*/;

    public static native void flushDom()
    /*-{
     $wnd.Polymer.dom.flush();
    }-*/;

    @JsType
    public interface TokenList {
        @JsProperty
        public int getLength();

        public String item(int index);

        public boolean contains(String token);

        public void add(String item);

        public void remove(String item);

        public boolean toggle(String item);
    }

    // See https://www.polymer-project.org/1.0/docs/devguide/local-dom.html
    @JsType
    public interface PolymerDOM {

        // Adding and removing children

        public void appendChild(Node childNode);

        public void insertBefore(Node child, Node beforeNode);

        public void removeChild(Node childNode);

        // Parent and child APIs

        @JsProperty
        public JsArrayObject<Node> getChildNodes();

        @JsProperty
        public Node getParentNode();

        @JsProperty
        public Node getFirstChild();

        @JsProperty
        public Node getLastChild();

        @JsProperty
        public Node getFirstElementChild();

        @JsProperty
        public Node getLastElementChild();

        @JsProperty
        public Node getPreviousSibling();

        @JsProperty
        public Node getNextSibling();

        @JsProperty
        public Node getTextContent();

        @JsProperty
        public Node getInnerHTML();

        // Query selector
        public Node querySelector(String selector);

        public JsArrayObject<Node> querySelectorAll(String selector);

        // Content APIs
        // public ??? getDistributedNodes()
        // public ??? getDestinationInsertionPoints()

        // Node mutation APIs
        public void setAttribute(String attribute, String value);

        public void removeAttribute(String attribute);

        @JsProperty
        public TokenList getClassList();

    }

}
