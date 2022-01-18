/*
 * Copyright 2000-2022 Vaadin Ltd.
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
package com.vaadin.flow.shared.internal;

import java.io.Serializable;

/**
 * Registry of node feature id numbers and map keys shared between server and
 * client.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public final class NodeFeatures {
    /**
     * Id for ElementData.
     */
    public static final int ELEMENT_DATA = 0;

    /**
     * Id for ElementPropertyMap.
     */
    public static final int ELEMENT_PROPERTIES = 1;

    /**
     * Id for ElementChildrenList.
     */
    public static final int ELEMENT_CHILDREN = 2;

    /**
     * Id for ElementAttributeMap.
     */
    public static final int ELEMENT_ATTRIBUTES = 3;

    /**
     * Id for ElementListenerMap.
     */
    public static final int ELEMENT_LISTENERS = 4;
    /**
     * Id for PushConfigurationMap.
     */
    public static final int UI_PUSHCONFIGURATION = 5;
    /**
     * Id for PushConfigurationParametersMap.
     */
    public static final int UI_PUSHCONFIGURATION_PARAMETERS = 6;
    /**
     * Id for TextNodeMap.
     */
    public static final int TEXT_NODE = 7;
    /**
     * Id for PollConfigurationMap.
     */
    public static final int POLL_CONFIGURATION = 8;
    /**
     * Id for ReconnectDialogConfigurationMap.
     */
    public static final int RECONNECT_DIALOG_CONFIGURATION = 9;
    /**
     * Id for ReconnectDialogConfigurationMap.
     */
    public static final int LOADING_INDICATOR_CONFIGURATION = 10;
    /**
     * Id for ElementClassList.
     */
    public static final int CLASS_LIST = 11;
    /**
     * Id for ElementStylePropertyMap.
     */
    public static final int ELEMENT_STYLE_PROPERTIES = 12;
    /**
     * Id for ComponentMapping.
     */
    public static final int COMPONENT_MAPPING = 15;
    /**
     * Id for ModelList.
     */
    public static final int TEMPLATE_MODELLIST = 16;

    /**
     * Id for PolymerServerEventHandlers.
     */
    public static final int POLYMER_SERVER_EVENT_HANDLERS = 17;

    /**
     * Id for PolymerEventListenerMap.
     */
    public static final int POLYMER_EVENT_LISTENERS = 18;

    /**
     * Id for ClientCallableHandlers.
     */
    public static final int CLIENT_DELEGATE_HANDLERS = 19;

    /**
     * Id for ShadowRootData.
     */
    public static final int SHADOW_ROOT_DATA = 20;

    /**
     * Id for ShadowRootHost.
     */
    public static final int SHADOW_ROOT_HOST = 21;

    /**
     * Id for AttachExistingElementFeature.
     */
    public static final int ATTACH_EXISTING_ELEMENT = 22;

    /**
     * Id for BasicTypeValue.
     */
    public static final int BASIC_TYPE_VALUE = 23;

    /**
     * Id for VirtualChildrenList.
     */
    public static final int VIRTUAL_CHILDREN = 24;

    /**
     * Id for ReturnChannelMap.
     */
    public static final int RETURN_CHANNEL_MAP = 25;

    /**
     * Id for InertData.
     */
    public static final int INERT_DATA = 26;

    private NodeFeatures() {
        // Only static
    }
}
