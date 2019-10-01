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
package com.vaadin.flow.shared;

import java.io.Serializable;

import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.dom.DebouncePhase;

/**
 * A utility class providing constants for JSON related features.
 *
 * @author Vaadin Ltd
 * @since 1.0
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
     * Change type for empty change (populate the feature on the client side
     * only).
     */
    public static final String CHANGE_TYPE_NOOP = "empty";

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
     * Change type for list clear changes.
     */
    public static final String CHANGE_TYPE_CLEAR = "clear";

    /**
     * Key holding the feature of a change.
     */
    public static final String CHANGE_FEATURE = "feat";

    /**
     * Key holding the feature type.
     */
    public static final String CHANGE_FEATURE_TYPE = "featType";

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
     * Type value for navigation events from the client.
     */
    public static final String RPC_TYPE_NAVIGATION = "navigation";

    /**
     * Key holding the node in messages sent from the client.
     */
    public static final String RPC_NODE = "node";

    /**
     * Key holding the event type in event messages sent from the client.
     */
    public static final String RPC_EVENT_TYPE = "event";

    /**
     * Type value for model map synchronizations sent from the client.
     */
    public static final String RPC_TYPE_MAP_SYNC = "mSync";

    /**
     * Key holding the event data in event messages sent from the client.
     */
    public static final String RPC_EVENT_DATA = "data";

    /**
     * Key used for data related to
     * {@link Page#executeJs(String, Serializable...)} in UIDL messages.
     */
    public static final String UIDL_KEY_EXECUTE = "execute";

    /**
     * Key used to hold the feature id when synchronizing node values.
     */
    public static final String RPC_FEATURE = "feature";

    /**
     * Key used to hold the name of the synchronized property.
     */
    public static final String RPC_PROPERTY = "property";

    /**
     * Key used to hold the value of the synchronized property.
     */
    public static final String RPC_PROPERTY_VALUE = "value";

    /**
     * Key used to hold the location in a navigation message.
     */
    public static final String RPC_NAVIGATION_LOCATION = "location";

    /**
     * Key used to hold the state in a navigation message.
     */
    public static final String RPC_NAVIGATION_STATE = "state";

    /**
     * Key used in navigation messages triggered by a router link.
     */
    public static final String RPC_NAVIGATION_ROUTERLINK = "link";

    /**
     * Type value for events sent from the client to an event handler published
     * on the server.
     */
    public static final String RPC_PUBLISHED_SERVER_EVENT_HANDLER = "publishedEventHandler";

    /**
     * Key used to hold the server side method name in template event messages
     * sent from the client.
     */
    public static final String RPC_TEMPLATE_EVENT_METHOD_NAME = "templateEventMethodName";

    /**
     * Key used to hold the argument values for server side method call.
     *
     * @see #RPC_TEMPLATE_EVENT_METHOD_NAME
     * @see #RPC_PUBLISHED_SERVER_EVENT_HANDLER
     */
    public static final String RPC_TEMPLATE_EVENT_ARGS = "templateEventMethodArgs";

    /**
     * Key used to hold the promise id for a server side method call.
     */
    public static final String RPC_TEMPLATE_EVENT_PROMISE = "promise";

    /**
     * Name of the $server property that is used to track pending promises. The
     * name is chosen to avoid conflicts with genuine $server method names.
     */
    public static final String RPC_PROMISE_CALLBACK_NAME = "}p";

    /**
     * Type value for attach existing element server callback.
     *
     * @see #RPC_ATTACH_ASSIGNED_ID
     * @see #RPC_ATTACH_REQUESTED_ID
     * @see #RPC_ATTACH_TAG_NAME
     * @see #RPC_ATTACH_INDEX
     */
    public static final String RPC_ATTACH_EXISTING_ELEMENT = "attachExistingElement";

    /**
     * Type value for attach existing element server callback.
     *
     * @see #RPC_ATTACH_ASSIGNED_ID
     * @see #RPC_ATTACH_REQUESTED_ID
     * @see #RPC_ATTACH_TAG_NAME
     * @see #RPC_ATTACH_ID
     */
    public static final String RPC_ATTACH_EXISTING_ELEMENT_BY_ID = "attachExistingElementById";

    /**
     * Key used to hold requested state node identifier for attach existing
     * element request.
     */
    public static final String RPC_ATTACH_REQUESTED_ID = "attachReqId";

    /**
     * Key used to hold assigned state node identifier for attach existing
     * element request.
     */
    public static final String RPC_ATTACH_ASSIGNED_ID = "attachAssignedId";

    /**
     * Key used to hold tag name for attach existing element request.
     */
    public static final String RPC_ATTACH_TAG_NAME = "attachTagName";

    /**
     * Key used to hold index of server side element for attach existing element
     * request.
     */
    public static final String RPC_ATTACH_INDEX = "attachIndex";

    /**
     * Key used to hold id of the element for attach existing element request.
     */
    public static final String RPC_ATTACH_ID = "attachId";

    /**
     * Key used when informing the client side that the session has expired.
     */
    public static final String META_SESSION_EXPIRED = "sessionExpired";

    /**
     * Key holding the debounce phase for an event data map from the client.
     */
    // "for" is a reserved keyword, which means that this cannot be a valid JS
    // expression, thus eliminating the risk for an accidental collision with a
    // genuine data expression
    public static final String EVENT_DATA_PHASE = "for";

    /**
     * Character used for representing {@link DebouncePhase#LEADING}.
     */
    public static final String EVENT_PHASE_LEADING = "leading";

    /**
     * Character used for representing {@link DebouncePhase#INTERMEDIATE}.
     */
    public static final String EVENT_PHASE_INTERMEDIATE = "intermediate";

    /**
     * Character used for representing {@link DebouncePhase#TRAILING}.
     */
    public static final String EVENT_PHASE_TRAILING = "trailing";

    /**
     * Token used as an event data expression to represent that properties
     * should be synchronized. The token is chosen to avoid collisions with
     * regular event data expressions by using a character that cannot be the
     * start of a valid JS expression.
     */
    public static final String SYNCHRONIZE_PROPERTY_TOKEN = "}";

    /**
     * RPC type value used for return channel messages.
     */
    public static final String RPC_TYPE_CHANNEL = "channel";

    /**
     * Key for the channel id in return channel messages.
     */
    public static final String RPC_CHANNEL = "channel";

    /**
     * Key for the arguments array in return channel messages.
     */
    public static final String RPC_CHANNEL_ARGUMENTS = "args";

}
