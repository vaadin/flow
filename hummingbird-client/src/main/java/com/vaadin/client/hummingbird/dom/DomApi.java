/*
 * Copyright 2000-2016 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.client.hummingbird.dom;

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
 */
public class DomApi {

    /**
     * Flag for tracking if Polymer-micro.html is loaded (contains dom).
     *
     * Package protected for testing reasons.
     */
    static boolean polymerMicroLoaded = false;

    /**
     * Flag for tracking if Polymer.html is loaded (contains updateStyles)
     *
     * Package protected for testing reasons.
     */
    static boolean polymerFullyLoaded = false;

    /**
     * The currently used DOM API implementation. By default just returns the
     * same object.
     *
     * Package protected for testing reasons.
     */
    static DomApiImpl impl = node -> (DomElement) node;

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
        return impl.wrap(node);
    }

    /**
     * Updates the DOM API implementation used.
     */
    public static void updateApiImplementation() {
        Console.log("Updating DomApi implementation");
        if (polymerFullyLoaded) {
            return;
        }

        if (PolymerDomApiImpl.isPolymerFullyLoaded()) {
            // Full Polymer loaded
            if (!polymerMicroLoaded) {
                // Full loads micro automatically
                polymerMicroLoaded();
            }
            polymerFullyLoaded = true;
            polymerFullyLoaded();
        } else if (!polymerMicroLoaded
                && PolymerDomApiImpl.isPolymerMicroLoaded()) {
            // Only micro loaded
            polymerMicroLoaded();
        }
    }

    private static void polymerMicroLoaded() {
        polymerMicroLoaded = true;
        Console.log("Polymer micro is now loaded, using Polymer DOM API");
        impl = new PolymerDomApiImpl();
    }

    private static void polymerFullyLoaded() {
        polymerFullyLoaded = true;
        Console.log("Polymer is now fully loaded");
    }

}
