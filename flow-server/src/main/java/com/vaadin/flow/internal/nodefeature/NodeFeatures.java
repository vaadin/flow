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
package com.vaadin.flow.internal.nodefeature;

import com.vaadin.flow.internal.nodefeature.PushConfigurationMap.PushConfigurationParametersMap;

/**
 * Registry of node feature id numbers and map keys shared between server and
 * client.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public final class NodeFeatures {
    /**
     * Id for {@link ElementData}.
     */
    public static final int ELEMENT_DATA = 0;

    /**
     * Id for {@link ElementPropertyMap}.
     */
    public static final int ELEMENT_PROPERTIES = 1;

    /**
     * Id for {@link ElementChildrenList}.
     */
    public static final int ELEMENT_CHILDREN = 2;

    /**
     * Id for {@link ElementAttributeMap}.
     */
    public static final int ELEMENT_ATTRIBUTES = 3;

    /**
     * Id for {@link ElementListenerMap}.
     */
    public static final int ELEMENT_LISTENERS = 4;
    /**
     * Id for {@link PushConfigurationMap}.
     */
    public static final int UI_PUSHCONFIGURATION = 5;
    /**
     * Id for {@link PushConfigurationParametersMap}.
     */
    public static final int UI_PUSHCONFIGURATION_PARAMETERS = 6;
    /**
     * Id for {@link TextNodeMap}.
     */
    public static final int TEXT_NODE = 7;
    /**
     * Id for {@link PollConfigurationMap}.
     */
    public static final int POLL_CONFIGURATION = 8;
    /**
     * Id for {@link ReconnectDialogConfigurationMap}.
     */
    public static final int RECONNECT_DIALOG_CONFIGURATION = 9;
    /**
     * Id for {@link ReconnectDialogConfigurationMap}.
     */
    public static final int LOADING_INDICATOR_CONFIGURATION = 10;
    /**
     * Id for {@link ElementClassList}.
     */
    public static final int CLASS_LIST = 11;
    /**
     * Id for {@link ElementStylePropertyMap}.
     */
    public static final int ELEMENT_STYLE_PROPERTIES = 12;
    /**
     * Id for {@link SynchronizedPropertiesList}.
     */
    public static final int SYNCHRONIZED_PROPERTIES = 13;
    /**
     * Id for {@link SynchronizedPropertyEventsList}.
     */
    public static final int SYNCHRONIZED_PROPERTY_EVENTS = 14;
    /**
     * Id for {@link ComponentMapping}.
     */
    public static final int COMPONENT_MAPPING = 15;
    /**
     * Id for {@link ModelList}.
     */
    public static final int TEMPLATE_MODELLIST = 16;

    /**
     * Id for {@link PolymerServerEventHandlers}.
     */
    public static final int POLYMER_SERVER_EVENT_HANDLERS = 17;

    /**
     * Id for {@link PolymerEventListenerMap}.
     */
    public static final int POLYMER_EVENT_LISTENERS = 18;

    /**
     * Id for {@link ClientCallableHandlers}.
     */
    public static final int CLIENT_DELEGATE_HANDLERS = 19;

    /**
     * Id for {@link ShadowRootData}.
     */
    public static final int SHADOW_ROOT_DATA = 20;

    /**
     * Id for {@link ShadowRootHost}.
     */
    public static final int SHADOW_ROOT_HOST = 21;

    /**
     * Id for {@link AttachExistingElementFeature}.
     */
    public static final int ATTACH_EXISTING_ELEMENT = 22;

    /**
     * Id for {@link BasicTypeValue}.
     */
    public static final int BASIC_TYPE_VALUE = 23;

    /**
     * Id for {@link VirtualChildrenList}.
     */
    public static final int VIRTUAL_CHILDREN = 24;

    /**
     * Id for {@link ReturnChannelMap}.
     */
    public static final int RETURN_CHANNEL_MAP = 25;

    private NodeFeatures() {
        // Only static
    }
}
