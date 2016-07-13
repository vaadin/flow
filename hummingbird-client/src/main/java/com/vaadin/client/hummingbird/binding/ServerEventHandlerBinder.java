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
package com.vaadin.client.hummingbird.binding;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.nodefeature.NodeList;
import com.vaadin.hummingbird.shared.NodeFeatures;

import elemental.dom.Element;
import elemental.events.EventRemover;

/**
 * Binds and updates <code>element.$server</code>.
 *
 * @author Vaadin Ltd
 */
public class ServerEventHandlerBinder {

    /**
     * A representation of <code>element.$server</code>.
     *
     * @author Vaadin Ltd
     */
    public static final class ServerEventObject extends JavaScriptObject {

        /**
         * JSO constructor.
         */
        protected ServerEventObject() {

        }

        /**
         * Defines a method with the given name to be a callback to the server
         * for the given state node.
         *
         * @param methodName
         *            the name of the method to add
         * @param node
         *            the node to use as an identifier when sending an event to
         *            the server
         */
        public native void defineMethod(String methodName, StateNode node)
        /*-{
            this[methodName] = $entry(function() {
                var args = Array.prototype.slice.call(arguments);
                var tree = node.@com.vaadin.client.hummingbird.StateNode::getTree()();
                tree.@com.vaadin.client.hummingbird.StateTree::sendTemplateEventToServer(*)(node, methodName, args);
            });
        }-*/;

        /**
         * Removes a method with the given name.
         *
         * @param methodName
         *            the name of the method to remove
         */
        public native void removeMethod(String methodName)
        /*-{
           delete this[methodName];
        }-*/;

        /**
         * Gets the defined methods.
         *
         * @return an array of defined method names
         */
        public native JsArray<String> getMethods()
        /*-{
           return Object.keys(this);
        }-*/;

        /**
         * Gets or creates <code>element.$server</code> for the given element.
         *
         * @param element
         *            the element to use
         * @return a reference to the <code>$server</code> object in the element
         */
        public static ServerEventObject get(Element element) {
            ServerEventObject serverObject = WidgetUtil
                    .crazyJsoCast(WidgetUtil.getJsProperty(element, "$server"));
            if (serverObject == null) {
                serverObject = (ServerEventObject) JavaScriptObject
                        .createObject();
                WidgetUtil.setJsProperty(element, "$server", serverObject);
            }
            return serverObject;
        }

    }

    private ServerEventHandlerBinder() {
        // Only static methods
    }

    /**
     * Registers all the server event handler names found in the
     * {@link NodeFeatures#PUBLISHED_SERVER_EVENT_HANDLERS} feature in the state
     * node as <code>element.$server.&lt;methodName&gt;</code>. Additionally
     * listens to changes in the feature and updates <code>$server</code>
     * accordingly.
     *
     * @param element
     *            the element to update
     * @param node
     *            the state node containing the feature
     * @return a handle which can be used to remove the listener for the feature
     */
    public static EventRemover bindServerEventHandlerNames(Element element,
            StateNode node) {
        NodeList serverEventHandlerNamesList = node
                .getList(NodeFeatures.PUBLISHED_SERVER_EVENT_HANDLERS);

        for (int i = 0; i < serverEventHandlerNamesList.length(); i++) {
            String serverEventHandlerName = (String) serverEventHandlerNamesList
                    .get(i);
            ServerEventObject.get(element).defineMethod(serverEventHandlerName,
                    node);
        }

        return serverEventHandlerNamesList.addSpliceListener(e -> {
            JsArray<?> remove = e.getRemove();
            for (int i = 0; i < remove.length(); i++) {
                ServerEventObject.get(element)
                        .removeMethod((String) remove.get(i));
            }

            JsArray<?> add = e.getAdd();
            for (int i = 0; i < add.length(); i++) {
                ServerEventObject.get(element).defineMethod((String) add.get(i),
                        node);
            }
        });
    }

}
