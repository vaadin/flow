package com.vaadin.client;

import elemental.dom.Element;
import elemental.dom.Node;

public class ElementUtil {
    /**
     * Checks whether the {@code node} has required {@code tag}.
     *
     * @param node
     *            the node to check
     * @param tag
     *            the required tag name
     * @return {@code true} if the node has required tag name
     */
    public static boolean hasTag(Node node, String tag) {
        return node instanceof Element
                && tag.equalsIgnoreCase(((Element) node).getTagName());
    }

    /**
     * Searches the shadow root of the given context element for the given id or
     * searches the light DOM if the element has no shadow root.
     *
     * @param context
     *            the container element to search through
     * @param id
     *            the identifier of the element to search for
     * @return the element with the given {@code id} if found, otherwise
     *         <code>null</code>
     */
    public static native Element getElementById(Node context, String id)
    /*-{
       var query = "#" + id;

       if (context.shadowRoot) {
         return context.shadowRoot.querySelector(query);
       } else if (context.querySelector) {
         return context.querySelector(query);
       } else {
         return null;
       }
    }-*/;

}
