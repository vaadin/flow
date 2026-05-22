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

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

import com.vaadin.client.flow.StateNode;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;

import elemental.dom.Element;
import elemental.events.EventRemover;

/**
 * Binds and updates server-event handler names. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/flow/binding/ServerEventHandlerBinder.ts}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow.binding", name = "ServerEventHandlerBinder")
public final class ServerEventHandlerBinder {

    /** Supplier shape used by {@link #bindServerEventHandlerNames}. */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    public interface ServerEventObjectProvider {
        ServerEventObject get();
    }

    private ServerEventHandlerBinder() {
    }

    /**
     * Registers all the server event handler names found in the given feature
     * on the {@link ServerEventObject} produced by {@code objectProvider},
     * keeping the registration in sync with subsequent splice updates.
     */
    public static native EventRemover bindServerEventHandlerNames(
            ServerEventObjectProvider objectProvider, StateNode node,
            int featureId, boolean returnValue);

    /**
     * Convenience wrapper that targets the {@code element.$server} object and
     * the {@code CLIENT_DELEGATE_HANDLERS} feature with promise return values.
     */
    @JsOverlay
    public static EventRemover bindServerEventHandlerNames(Element element,
            StateNode node) {
        return bindServerEventHandlerNames(() -> ServerEventObject.get(element),
                node, NodeFeatures.CLIENT_DELEGATE_HANDLERS, true);
    }
}
