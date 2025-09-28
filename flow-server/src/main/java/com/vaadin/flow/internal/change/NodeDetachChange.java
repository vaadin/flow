/*
 * Copyright 2000-2025 Vaadin Ltd.
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

package com.vaadin.flow.internal.change;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.ConstantPool;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.JsonConstants;

/**
 * Change describing that a node has been detached.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class NodeDetachChange extends NodeChange {
    /**
     * Creates a new detach change.
     *
     * @param node
     *            the detached node
     */
    public NodeDetachChange(StateNode node) {
        super(node);
    }

    @Override
    protected void populateJson(ObjectNode json, ConstantPool constantPool) {
        json.put(JsonConstants.CHANGE_TYPE, JsonConstants.CHANGE_TYPE_DETACH);
    }
}
