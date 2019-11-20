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
package com.vaadin.client.flow.binding;

import java.util.function.Supplier;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;

import elemental.dom.Element;
import elemental.events.EventRemover;

/**
 * Binds and updates server object able to send notifications to the server.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ServerEventHandlerBinder {

    private ServerEventHandlerBinder() {
        // Only static methods
    }

    /**
     * Registers all the server event handler names found in the
     * {@link NodeFeatures#CLIENT_DELEGATE_HANDLERS} feature in the state node
     * as <code>serverObject.&lt;methodName&gt;</code>. Additionally listens to
     * changes in the feature and updates <code>$server</code> accordingly.
     *
     * @param element
     *            the element to update
     * @param node
     *            the state node containing the feature
     * @return a handle which can be used to remove the listener for the feature
     */
    public static EventRemover bindServerEventHandlerNames(Element element,
            StateNode node) {
        return bindServerEventHandlerNames(() -> ServerEventObject.get(element),
                node, NodeFeatures.CLIENT_DELEGATE_HANDLERS, true);
    }

    /**
     * Registers all the server event handler names found in the feature with
     * the {@code featureId} in the {@link ServerEventObject} {@code object}.
     * Additionally listens to changes in the feature and updates server event
     * object accordingly.
     *
     * @param objectProvider
     *            the provider of the event object to update
     * @param node
     *            the state node containing the feature
     * @param featureId
     *            the feature id which contains event handler methods
     * @param returnValue
     *            <code>true</code> if the handler should return a promise that
     *            will reflect the server-side result; <code>false</code> to not
     *            return any value
     * @return a handle which can be used to remove the listener for the feature
     */
    public static EventRemover bindServerEventHandlerNames(
            Supplier<ServerEventObject> objectProvider, StateNode node,
            int featureId, boolean returnValue) {
        NodeList serverEventHandlerNamesList = node.getList(featureId);

        if (serverEventHandlerNamesList.length() > 0) {
            ServerEventObject object = objectProvider.get();

            for (int i = 0; i < serverEventHandlerNamesList.length(); i++) {
                String serverEventHandlerName = (String) serverEventHandlerNamesList
                        .get(i);
                object.defineMethod(serverEventHandlerName, node, returnValue);
            }
        }

        return serverEventHandlerNamesList.addSpliceListener(e -> {
            ServerEventObject serverObject = objectProvider.get();

            JsArray<?> remove = e.getRemove();
            for (int i = 0; i < remove.length(); i++) {
                serverObject.removeMethod((String) remove.get(i));
            }

            JsArray<?> add = e.getAdd();
            for (int i = 0; i < add.length(); i++) {
                serverObject.defineMethod((String) add.get(i), node,
                        returnValue);
            }
        });
    }
}
