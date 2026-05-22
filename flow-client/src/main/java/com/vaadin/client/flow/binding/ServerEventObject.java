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
package com.vaadin.client.flow.binding;

import jsinterop.annotations.JsType;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.collection.JsArray;

import elemental.dom.Element;
import elemental.dom.Node;

/**
 * Server-handler bag attached to a host element under {@code $server}. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/flow/binding/ServerEventObject.ts}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow.binding", name = "ServerEventObject")
public class ServerEventObject {

    /**
     * Defines a server-callable method with the given name. When invoked
     * client-side, the call is forwarded via
     * {@code StateTree.sendTemplateEventToServer}.
     */
    public native void defineMethod(String methodName, StateNode node,
            boolean returnPromise);

    /** Removes a previously defined method. */
    public native void removeMethod(String methodName);

    /** Returns the names of all defined methods. */
    public native JsArray<String> getMethods();

    /**
     * Rejects all promises currently pending on this server object. Called
     * during client resynchronization.
     */
    public native void rejectPromises();

    /** Gets or creates the {@code element.$server} object. */
    public static native ServerEventObject get(Element element);

    /** Returns {@code node.$server} if attached, {@code null} otherwise. */
    public static native ServerEventObject getIfPresent(Node node);
}
