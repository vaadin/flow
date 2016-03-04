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
package com.vaadin.shared;

import java.io.Serializable;

import com.vaadin.ui.Page;

/**
 * A utility class providing constants for JSON related features.
 *
 * @author Vaadin
 * @since
 */
public class JsonConstants implements Serializable {

    /**
     * The content type for JSON requests/responses.
     */
    public static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

    /**
     * Key holding id of the node affected by a change.
     */
    public static final String CHANGE_NODE = "node";

    /**
     * Key holding the type of a change.
     */
    public static final String CHANGE_TYPE = "type";

    /**
     * Change type for attaching nodes.
     */
    public static final String CHANGE_TYPE_ATTACH = "attach";

    /**
     * Change type for detaching nodes.
     */
    public static final String CHANGE_TYPE_DETACH = "detach";

    /**
     * Change type for list splice changes.
     */
    public static final String CHANGE_TYPE_SPLICE = "splice";

    /**
     * Change type for map put changes.
     */
    public static final String CHANGE_TYPE_PUT = "put";

    /**
     * Change type for map remove changes.
     */
    public static final String CHANGE_TYPE_REMOVE = "remove";

    /**
     * Key holding the namespace of a change.
     */
    public static final String CHANGE_NAMESPACE = "ns";

    /**
     * Key holding the map key of the change.
     */
    public static final String CHANGE_MAP_KEY = "key";

    /**
     * Key holding nodes to add for a splice.
     */
    public static final String CHANGE_SPLICE_ADD_NODES = "addNodes";

    /**
     * Key holding values to add for a splice.
     */
    public static final String CHANGE_SPLICE_ADD = "add";

    /**
     * Key holding the number of items to remove for a splice.
     */
    public static final String CHANGE_SPLICE_REMOVE = "remove";

    /**
     * Key holding the index of a splice.
     */
    public static final String CHANGE_SPLICE_INDEX = "index";

    /**
     * Key holding the value of a put change.
     */
    public static final String CHANGE_PUT_VALUE = "value";

    /**
     * Key holder the node value of a put change.
     */
    public static final String CHANGE_PUT_NODE_VALUE = "nodeValue";

    /**
     * Key holding the type in of messages sent from the client.
     */
    public static final String RPC_TYPE = "type";

    /**
     * Type value for events sent from the client.
     */
    public static final String RPC_TYPE_EVENT = "event";

    /**
     * Type value for popstate events from the client.
     */
    public static final String RPC_TYPE_POPSTATE = "popstate";

    /**
     * Key holding the node in messages sent from the client.
     */
    public static final String RPC_NODE = "node";

    /**
     * Key holding the event type in event messages sent from the client.
     */
    public static final String RPC_EVENT_TYPE = "event";

    /**
     * Type value for property synchronizations sent from the client.
     */
    public static final String RPC_TYPE_PROPERTY_SYNC = "pSync";

    /**
     * Key holding the event data in event messages sent from the client.
     */
    public static final String RPC_EVENT_DATA = "data";

    /**
     * Key used for data related to
     * {@link Page#executeJavaScript(String, Object...)} in UIDL messages.
     */
    public static final String UIDL_KEY_EXECUTE = "execute";

    /**
     * Key used to hold the name of the synchronized property.
     */
    public static final String RPC_PROPERTY = "property";

    /**
     * Key used to hold the value of the synchronized property.
     */
    public static final String RPC_PROPERTY_VALUE = "value";

    /**
     * Key used to hold the location in a popstate message.
     */
    public static final String RPC_POPSTATE_LOCATION = "location";

    /**
     * Key used to hold the state in a popstate message.
     */
    public static final String RPC_POPSTATE_STATE = "state";

    /**
     * Type value for routing events sent from client.
     */
    public static final String RPC_TYPE_ROUTING = "routing";

}
