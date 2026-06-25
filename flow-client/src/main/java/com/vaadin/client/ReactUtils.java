/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client;

import java.util.function.Supplier;

import elemental.dom.Element;

/**
 * Utils class, intended to ease working with React component related code on
 * the client side.
 *
 * @author Vaadin Ltd
 * @since 24.5.
 */
public final class ReactUtils {

    /**
     * Add a callback to the react component that is called when the component
     * initialization is ready for binding flow.
     *
     * @param element
     *            react component element
     * @param name
     *            name of container to bind to
     * @param runnable
     *            callback function runnable
     */
    public static native void addReadyCallback(Element element, String name,
            Runnable runnable)
    /*-{
            if(element.addReadyCallback){
                element.addReadyCallback(name,
                    $entry(runnable.@java.lang.Runnable::run(*).bind(runnable))
                );
            }
    }-*/;

    /**
     * Check if the react element is initialized and functional.
     *
     * @param elementLookup
     *            react element lookup supplier
     * @return {@code true} if Flow binding can already be done
     */
    public static boolean isInitialized(Supplier<Element> elementLookup) {
        return elementLookup.get() != null;
    }
}
