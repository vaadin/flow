/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.communication.ServerRpcHandler;
import com.vaadin.flow.server.communication.UidlRequestHandler;
import com.vaadin.flow.server.communication.rpc.MapSyncRpcHandler;
import com.vaadin.flow.server.communication.rpc.RpcInvocationHandler;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonObject;

@WebServlet(asyncSupported = true, urlPatterns = { "/sync-rpc/*" })
public class MapSyncRpcHandlerTestServlet extends VaadinServlet {

    private static class MapSyncServletService extends VaadinServletService {

        public MapSyncServletService(VaadinServlet servlet,
                DeploymentConfiguration deploymentConfiguration) {
            super(servlet, deploymentConfiguration);
        }

        @Override
        protected List<RequestHandler> createRequestHandlers()
                throws ServiceException {
            List<RequestHandler> handlers = super.createRequestHandlers();
            ArrayList<RequestHandler> newHandlers = new ArrayList<>(
                    handlers.size());
            for (int i = 0; i < handlers.size(); i++) {
                RequestHandler handler = handlers.get(i);
                if (handler instanceof UidlRequestHandler) {
                    newHandlers.add(new MapSyncUidlRequestHandler());
                } else {
                    newHandlers.add(handler);
                }
            }
            return newHandlers;
        }

    }

    private static class MapSyncUidlRequestHandler extends UidlRequestHandler {

        @Override
        protected ServerRpcHandler createRpcHandler() {
            return new MapSyncServerRpcHandler();
        }
    }

    private static class MapSyncServerRpcHandler extends ServerRpcHandler {
        @Override
        protected Map<String, RpcInvocationHandler> getInvocationHandlers() {
            Map<String, RpcInvocationHandler> map = super.getInvocationHandlers();

            map = new HashMap<>(map);
            map.put(JsonConstants.RPC_TYPE_MAP_SYNC,
                    new TestMapSyncRpcHandler());
            return map;
        }
    }

    private static class TestMapSyncRpcHandler extends MapSyncRpcHandler {

        @Override
        protected Optional<Runnable> handleNode(StateNode node,
                JsonObject invocationJson) {
            String property = invocationJson
                    .getString(JsonConstants.RPC_PROPERTY);
            if (property.equals("value")) {
                // Replace 'value' property with fake unsync property
                invocationJson.put(JsonConstants.RPC_PROPERTY, "foo");
            }
            return super.handleNode(node, invocationJson);
        }
    }

    @Override
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        MapSyncServletService service = new MapSyncServletService(this,
                deploymentConfiguration);
        service.init();
        return service;
    }
}
