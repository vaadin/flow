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
package com.vaadin.client.communication;

import jsinterop.annotations.JsType;

import com.vaadin.client.flow.StateNode;

/**
 * Observes the loading indicator configuration stored in the given node and
 * configures the loading indicator accordingly. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/communication/LoadingIndicatorConfigurator.ts}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.communication", name = "LoadingIndicatorConfigurator")
public final class LoadingIndicatorConfigurator {

    private LoadingIndicatorConfigurator() {
        // No instance should ever be created.
    }

    /**
     * Observes the given node for loading indicator configuration changes and
     * configures the loading indicator singleton accordingly.
     *
     * @param node
     *            the node containing the loading indicator configuration
     */
    public static native void observe(StateNode node);
}
