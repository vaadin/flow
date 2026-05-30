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

import elemental.dom.Node;

/**
 * Entry point for binding {@link Node} to state nodes. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/flow/binding/Binder.ts}.
 * <p>
 * This is the only public API class for external use.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow.binding", name = "Binder")
public final class Binder {

    private Binder() {
    }

    /**
     * Bind the {@code domNode} to the {@code stateNode}.
     *
     * @param stateNode
     *            the state node
     * @param domNode
     *            the DOM node to bind, not {@code null}
     */
    public static native void bind(StateNode stateNode, Node domNode);
}
