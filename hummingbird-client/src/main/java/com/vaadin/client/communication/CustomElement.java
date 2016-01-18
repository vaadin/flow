package com.vaadin.client.communication;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

public class CustomElement {

    public static Element createPolymerStyleModule(String styleModuleName) {
        Element element = createElement("style", "custom-style");
        element.setAttribute("include", styleModuleName);
        return element;
    }

    public static Element createElement(String tag, String is) {
        return createElement(Document.get(), tag, is);
    }

    public static native Element createElement(Document doc, String tag,
            String is)

    /*-{
        return doc.createElement(tag, is);
    }-*/;
}
