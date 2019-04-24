/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.client;

import elemental.dom.Element;
import elemental.dom.Node;
import elemental.json.JsonArray;

/**
 * Utils class, intended to ease working with Lit related code on client side.
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
            function () {
                runnable.@java.lang.Runnable::run(*)();
            });
    }-*/;

    public static native Element getElementById(Element element, String id)
    /*-{
        if (element.shadowRoot) {
            return element.shadowRoot.getElementById(id);
        } else {
            return element.querySelector("#"+id);
        }
    }-*/;

    /**
     * Splices the given array property and ensures Lit element is aware of the
     * splice.
     *
     * @param htmlNode
     *            node to call splice method on
     * @param path
     *            model path to property
     * @param startIndex
     *            start index of a list for splice operation
     * @param deleteCount
     *            number of elements to delete from the list after startIndex
     * @param itemsToAdd
     *            elements to add after startIndex
     *
     */
    public static native void splice(Element htmlNode, String path,
            int startIndex, int deleteCount, JsonArray itemsToAdd)
    /*-{
        paths = path.split("\.");
        var obj = htmlNode;
        for (i=0; i < paths.length; i++) {
            obj = obj[paths[i]];
        }
        obj.splice.apply(obj, [startIndex, deleteCount].concat(itemsToAdd));
        htmlNode.requestUpdate(paths[0]);
    }-*/;

}
