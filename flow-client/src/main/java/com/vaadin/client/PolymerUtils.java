/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import jsinterop.annotations.JsType;

import com.vaadin.client.flow.StateNode;

import elemental.dom.Element;
import elemental.dom.Node;
import elemental.dom.ShadowRoot;
import elemental.json.JsonArray;
import elemental.json.JsonValue;

/**
 * Utils class for working with Polymer-related code on the client. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/PolymerUtils.ts}.
 *
 * @author Vaadin Ltd
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "PolymerUtils")
public final class PolymerUtils {

    private PolymerUtils() {
    }

    /** Sets a new value for the list element of {@code htmlNode}. */
    public static native void setListValueByIndex(Element htmlNode, String path,
            int listIndex, JsonValue newValue);

    /** Calls Polymer {@code splice} on {@code htmlNode}. */
    public static native void splice(Element htmlNode, String path,
            int startIndex, int deleteCount, JsonArray itemsToAdd);

    /**
     * Records the {@code id} of a state node on the Polymer object at the given
     * model {@code path}.
     */
    public static native void storeNodeId(Node domNode, int id, String path);

    /**
     * Converts an arbitrary value (state node / map property / primitive) to
     * JSON.
     */
    public static native JsonValue createModelTree(Object object);

    /** @return whether the element is a Polymer 2 element. */
    public static native boolean isPolymerElement(Element htmlNode);

    /**
     * @return whether the element may turn into a Polymer 2 element later.
     * @deprecated unused.
     */
    @Deprecated
    public static native boolean mayBePolymerElement(Element htmlNode);

    /**
     * @deprecated unused.
     */
    @Deprecated
    public static native Node searchForElementInShadowRoot(
            ShadowRoot shadowRoot, String cssQuery);

    /**
     * @deprecated unused.
     */
    @Deprecated
    public static native Node getElementInShadowRootById(ShadowRoot shadowRoot,
            String id);

    /**
     * @deprecated use {@link ElementUtil#getElementById(Node, String)} for the
     *             generic version.
     */
    @Deprecated
    public static native Element getDomElementById(Node shadowRootParent,
            String id);

    /** @return whether the DOM of the polymer element is "ready". */
    public static native boolean isReady(Node shadowRootParent);

    /**
     * @deprecated use {@link ElementUtil#hasTag(Node, String)} instead.
     */
    @Deprecated
    public static native boolean hasTag(Node node, String tag);

    /**
     * Gets the custom element addressed by an array of child indices starting
     * from {@code root}.
     */
    public static native Element getCustomElement(Node root, JsonArray path);

    /** @return the shadow root of the {@code templateElement}. */
    public static native Element getDomRoot(Node templateElement);

    /**
     * Invokes {@code runnable} when the custom element with the given
     * {@code tagName} is initialized.
     */
    public static native void invokeWhenDefined(String tagName,
            Runnable runnable);

    /**
     * @return the tag name stored on the {@code node}'s ELEMENT_DATA feature.
     */
    public static native String getTag(StateNode node);

    /**
     * Subscribes a callback to run when {@code polymerElement} fires its ready
     * event.
     */
    public static native void addReadyListener(Element polymerElement,
            Runnable listener);

    /** Fires the ready event for the {@code polymerElement}. */
    public static native void fireReadyEvent(Element polymerElement);

    /** Sets a property on the element using Polymer's {@code set} method. */
    public static native void setProperty(Element element, String path,
            Object value);

    /** @return true iff the element has a shadow-root ancestor. */
    public static native boolean isInShadowRoot(Element element);
}
