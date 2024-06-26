/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
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
