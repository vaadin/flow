/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.server.communication.rpc;

import java.util.Optional;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonObject;

public class AbstractRpcInvocationHandlerTest {

    private static class TestRpcInvocationHandler
            extends AbstractRpcInvocationHandler {

        private StateNode node;

        @Override
        public String getRpcType() {
            return null;
        }

        @Override
        protected Optional<Runnable> handleNode(StateNode node,
                JsonObject invocationJson) {
            this.node = node;
            return Optional.empty();
        }
    };

    private TestRpcInvocationHandler handler = new TestRpcInvocationHandler();

    @Test
    public void handleVisibleAndEnabledNode_nodeIsHandled() {
        UI ui = new UI();

        Element element = createRpcInvocationData(ui, null);

        Assert.assertSame(element.getNode(), handler.node);
    }

    @Test
    public void handleInactiveNode_nodeIsNotHandled() {
        UI ui = new UI();

        createRpcInvocationData(ui, elem -> {
            elem.setVisible(false);
            elem.getNode().updateActiveState();

        });

        Assert.assertNull(handler.node);
    }

    private Element createRpcInvocationData(UI ui,
            Consumer<Element> addtionalConfig) {
        Element element = ElementFactory.createAnchor();
        ui.getElement().appendChild(element);

        if (addtionalConfig != null) {
            addtionalConfig.accept(element);
        }

        JsonObject object = Json.createObject();
        object.put(JsonConstants.RPC_NODE, element.getNode().getId());
        handler.handle(ui, object);
        return element;
    }
}
