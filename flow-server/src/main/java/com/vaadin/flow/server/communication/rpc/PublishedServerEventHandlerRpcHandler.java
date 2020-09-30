/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonObject;

/**
 * RPC handler for events triggered through <code>element.$server</code> or
 * simply <code>$server</code> in template event handlers.
 * <p>
 * The implementation is in the flow-polymer-template module since the handler
 * itslef is a part of the core and can't be moved but the logic is Polymer
 * related.
 *
 * @see JsonConstants#RPC_PUBLISHED_SERVER_EVENT_HANDLER
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @deprecated Polymer template support is deprecated - we recommend you to use
 *             {@code LitTemplate} instead. Read more details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
public class PublishedServerEventHandlerRpcHandler
        extends AbstractRpcInvocationHandler {

    @Override
    public String getRpcType() {
        return JsonConstants.RPC_PUBLISHED_SERVER_EVENT_HANDLER;
    }

    @Override
    public Optional<Runnable> handleNode(StateNode node,
            JsonObject invocationJson) {
        try {
            Class<?> clazz = Class
                    .forName("com.vaadin.flow.component.polymertemplate.rpc."
                            + PublishedServerEventHandlerRpcHandler.class
                                    .getSimpleName());
            Method handleMethod = Stream.of(clazz.getDeclaredMethods())
                    .filter(method -> Modifier.isStatic(method.getModifiers())
                            && Modifier.isPublic(method.getModifiers()))
                    .findFirst().get();
            handleMethod.invoke(null, node, invocationJson);
        } catch (ClassNotFoundException e) {
            LoggerFactory.getLogger(PublishedServerEventHandlerRpcHandler.class)
                    .debug("Polymer handler is called even though the polymer module is not in the classpath");
            // Just ignore: the functionality is not available
        } catch (IllegalAccessException e) {
            LoggerFactory.getLogger(PublishedServerEventHandlerRpcHandler.class)
                    .warn("Implemenation error occured", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        }

        return Optional.empty();
    }

}
