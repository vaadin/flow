/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client;

import elemental.dom.Element;
import elemental.dom.Node;

/**
 * Utils class, intended to ease working with LitElement related code on client
 * side.
 *
 * @author Vaadin Ltd
 */
public final class LitUtils {

    private LitUtils() {
    }

    /**
     * Checks if the given element is a LitElement.
     *
     * @param element
     *            the custom element
     * @return {@code true} if the element is a Lit element, <code>false</code>
     *         otherwise
     */
    public static native boolean isLitElement(Node element)
    /*-{
        return typeof element.update == "function" && element.updateComplete instanceof Promise && typeof element.shouldUpdate == "function" && typeof element.firstUpdated == "function";
    }-*/;

    /**
     * Invokes the {@code runnable} when the given Lit element has been rendered
     * at least once.
     *
     * @param element
     *            the Lit element
     * @param runnable
     *            the command to run
     */
    public static native void whenRendered(Element element, Runnable runnable)
    /*-{
        element.updateComplete.then(
            $entry(
              function() {
                runnable.@java.lang.Runnable::run(*)();
              })
            );
    }-*/;

}
