/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow.dom;

import com.vaadin.client.Console;

import elemental.dom.Node;

/**
 * Access point for DOM API. All operations and interactions with DOM nodes and
 * elements should go through this class.
 * <p>
 * This class delegates the operations to the actual DOM API implementations,
 * which might be changed on the run, meaning after dependencies have been
 * loaded.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DomApi {

    /**
     * Flag for tracking if Polymer-micro.html is loaded (contains dom).
     *
     * Package protected for testing reasons.
     */
    static boolean polymerMicroLoaded = false;

    /**
     * The currently used DOM API implementation. By default just returns the
     * same object.
     *
     * Package protected for testing reasons.
     */
    static DomApiImpl impl;

    private DomApi() {
        // NOOP
    }

    /**
     * Wraps the given DOM node to make it safe to invoke any of the methods
     * from {@link DomNode} or {@link DomElement}.
     *
     * @param node
     *            the node to wrap
     * @return a wrapped element
     */
    public static DomElement wrap(Node node) {
        if (impl == null) {
            return (DomElement) node;
        }
        return impl.wrap(node);
    }

    /**
     * Updates the DOM API implementation used.
     */
    public static void updateApiImplementation() {
        if (!polymerMicroLoaded && PolymerDomApiImpl.isPolymerMicroLoaded()) {
            polymerMicroLoaded();
        }
    }

    private static void polymerMicroLoaded() {
        polymerMicroLoaded = true;
        Console.log("Polymer micro is now loaded, using Polymer DOM API");
        impl = new PolymerDomApiImpl();
    }

}
