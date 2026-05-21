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

import com.vaadin.client.flow.collection.JsSet;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * Updates a state tree based on changes received from the server. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/flow/TreeChangeProcessor.ts}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow", name = "TreeChangeProcessor")
public class TreeChangeProcessor {

    private TreeChangeProcessor() {
        // Static-only.
    }

    /**
     * Update a state tree based on a JSON array of changes.
     *
     * @param tree
     *            the tree to update
     * @param changes
     *            the JSON array of changes
     * @return a set of updated nodes addressed by the {@code changes}
     */
    public static native JsSet<StateNode> processChanges(StateTree tree,
            JsonArray changes);

    /**
     * Update a state tree based on a single JSON change. Public for tests.
     *
     * @param tree
     *            the tree to update
     * @param change
     *            the JSON change
     * @return the updated node addressed by the change
     */
    public static native StateNode processChange(StateTree tree,
            JsonObject change);
}
