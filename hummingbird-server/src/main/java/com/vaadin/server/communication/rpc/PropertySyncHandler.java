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
package com.vaadin.server.communication.rpc;

import java.io.Serializable;

import com.vaadin.hummingbird.JsonCodec;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.ElementPropertyMap;
import com.vaadin.shared.JsonConstants;
import com.vaadin.ui.UI;

import elemental.json.JsonObject;

/**
 * Property synchronization RPC handler.
 * 
 * @see JsonConstants#RPC_TYPE_PROPERTY_SYNC
 * 
 * @author Vaadin Ltd
 *
 */
public class PropertySyncHandler extends AbstractInvocationHandler {

    @Override
    public String getRpcType() {
        return JsonConstants.RPC_TYPE_PROPERTY_SYNC;
    }

    @Override
    public void handle(UI ui, JsonObject invocationJson) {
        assert invocationJson.hasKey(JsonConstants.RPC_NODE);
        assert invocationJson.hasKey(JsonConstants.RPC_PROPERTY);
        assert invocationJson.hasKey(JsonConstants.RPC_PROPERTY_VALUE);

        StateNode node = getNode(ui, invocationJson);
        if (node == null) {
            return;
        }
        String property = invocationJson.getString(JsonConstants.RPC_PROPERTY);
        Serializable value = JsonCodec.decodeWithoutTypeInfo(
                invocationJson.get(JsonConstants.RPC_PROPERTY_VALUE));
        node.getFeature(ElementPropertyMap.class).setProperty(property, value,
                false);
    }

}
