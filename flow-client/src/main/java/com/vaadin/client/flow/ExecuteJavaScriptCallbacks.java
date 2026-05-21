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
package com.vaadin.client.flow;

import jsinterop.annotations.JsType;

import com.vaadin.client.ExecuteJavaScriptElementUtils;
import com.vaadin.client.flow.collection.JsArray;

import elemental.dom.Element;

/**
 * Per-execution element-handling callbacks delivered to the TypeScript-side
 * {@code ExecuteJavaScriptProcessor}. Implementations dispatch into
 * {@code ExecuteJavaScriptElementUtils} (whose helpers stay on the Java side as
 * {@code @JsOverlay} statics).
 */
@JsType
public interface ExecuteJavaScriptCallbacks {
    void attachExistingElement(StateNode parent, Element previousSibling,
            String tagName, int id);

    void populateModelProperties(StateNode node, JsArray<String> properties);

    void registerUpdatableModelProperties(StateNode node,
            JsArray<String> properties);

    void registerInitializer(StateNode node, double id,
            ExecuteJavaScriptElementUtils.JsCallback cleanup);

    void disposeInitializer(StateNode node, double id);
}
