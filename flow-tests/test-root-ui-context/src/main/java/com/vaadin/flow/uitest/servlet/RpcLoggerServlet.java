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
package com.vaadin.flow.uitest.servlet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.communication.ServerRpcHandler;
import com.vaadin.flow.server.communication.UidlRequestHandler;
import com.vaadin.flow.server.communication.rpc.RpcInvocationHandler;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonObject;

@WebServlet(asyncSupported = true, urlPatterns = { "/rpc/*" })
public class RpcLoggerServlet extends VaadinServlet {

    @Override
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        RPCLoggerService service = new RPCLoggerService(this,
                deploymentConfiguration);
        service.init();
        return service;
    }

    private static class RPCLoggerService extends VaadinServletService {

        public RPCLoggerService(VaadinServlet servlet,
                DeploymentConfiguration deploymentConfiguration)
                throws ServiceException {
            super(servlet, deploymentConfiguration);
        }

        @Override
        protected List<RequestHandler> createRequestHandlers()
                throws ServiceException {
            List<RequestHandler> handlers = super.createRequestHandlers();
            handlers.add(new LoggingUidlRequestHandler());
            return handlers;
        }

    }

    private static class LoggingUidlRequestHandler extends UidlRequestHandler {
        @Override
        protected ServerRpcHandler createRpcHandler() {
            return new LoggingServerRpcHandler();
        }
    }

    private static class LoggingServerRpcHandler extends ServerRpcHandler {

        private HashMap<String, RpcInvocationHandler> handlers;

        @Override
        protected Map<String, RpcInvocationHandler> getInvocationHandlers() {
            if (handlers == null) {
                handlers = new HashMap<>();
                super.getInvocationHandlers()
                        .forEach((type, handler) -> handlers.put(type,
                                new RpcInterceptor(handler)));

            }
            return handlers;
        }

    }

    private static class RpcInterceptor implements RpcInvocationHandler {

        private final RpcInvocationHandler delegate;

        private RpcInterceptor(RpcInvocationHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getRpcType() {
            return delegate.getRpcType();
        }

        @Override
        public Optional<Runnable> handle(UI ui, JsonObject invocationJson) {
            Optional<Runnable> runnable = delegate.handle(ui, invocationJson);
            StateNode node = ui.getInternals().getStateTree()
                    .getNodeById(getNodeId(invocationJson));
            Div container = new Div();
            container.addClassName("log");
            Label nodeLabel;
            if (node == null) {
                nodeLabel = new Label("Node is null");
            } else {
                nodeLabel = new Label("Node is " + node.getId());
            }
            nodeLabel.addClassName("node");
            container.add(nodeLabel);

            container.add(new Div());

            container.add(new Label("Invocation json is :"));
            Label json = new Label(invocationJson.toJson());
            json.addClassName("json");
            container.add(json);
            ui.add(container);

            return runnable;
        }

        private static int getNodeId(JsonObject invocationJson) {
            return (int) invocationJson.getNumber(JsonConstants.RPC_NODE);
        }

    }

}
