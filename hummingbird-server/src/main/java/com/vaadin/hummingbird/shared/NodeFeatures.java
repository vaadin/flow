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
package com.vaadin.hummingbird.shared;

import com.vaadin.hummingbird.nodefeature.ComponentMapping;
import com.vaadin.hummingbird.nodefeature.ElementAttributeMap;
import com.vaadin.hummingbird.nodefeature.ElementChildrenList;
import com.vaadin.hummingbird.nodefeature.ElementClassList;
import com.vaadin.hummingbird.nodefeature.ElementData;
import com.vaadin.hummingbird.nodefeature.ElementListenerMap;
import com.vaadin.hummingbird.nodefeature.ElementPropertyMap;
import com.vaadin.hummingbird.nodefeature.ElementStylePropertyMap;
import com.vaadin.hummingbird.nodefeature.ModelList;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.nodefeature.OverrideElementData;
import com.vaadin.hummingbird.nodefeature.ParentGeneratorHolder;
import com.vaadin.hummingbird.nodefeature.PollConfigurationMap;
import com.vaadin.hummingbird.nodefeature.PushConfigurationMap;
import com.vaadin.hummingbird.nodefeature.PushConfigurationMap.PushConfigurationParametersMap;
import com.vaadin.hummingbird.nodefeature.ReconnectDialogConfigurationMap;
import com.vaadin.hummingbird.nodefeature.SynchronizedPropertiesList;
import com.vaadin.hummingbird.nodefeature.SynchronizedPropertyEventsList;
import com.vaadin.hummingbird.nodefeature.TemplateMap;
import com.vaadin.hummingbird.nodefeature.TemplateEventHandlerNames;
import com.vaadin.hummingbird.nodefeature.TemplateOverridesMap;
import com.vaadin.hummingbird.nodefeature.TextNodeMap;

/**
 * Registry of node feature id numbers and map keys shared between server and
 * client.
 *
 * @author Vaadin Ltd
 */
public class NodeFeatures {
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
     * Id for {@link TemplateMap}.
     */
    public static final int TEMPLATE = 16;
    /**
     * Id for {@link ModelMap}.
     */
    public static final int TEMPLATE_MODELMAP = 17;
    /**
     * Id for {@link TemplateOverridesMap}.
     */
    public static final int TEMPLATE_OVERRIDES = 18;
    /**
     * Id for {@link OverrideElementData}.
     */
    public static final int OVERRIDE_DATA = 19;
    /**
     * Id for {@link ParentGeneratorHolder}.
     */
    public static final int PARENT_GENERATOR = 20;
    /**
     * Id for {@link ModelList}.
     */
    public static final int TEMPLATE_MODELLIST = 21;

    /**
     * Id for {@link TemplateEventHandlerNames}.
     */
    public static final int TEMPLATE_EVENT_HANDLER_NAMES = 22;

    /**
     * Key for {@link ElementData#getTag()}.
     */
    public static final String TAG = "tag";

    /**
     * Key for {@link TextNodeMap#getText()}.
     */
    public static final String TEXT = "text";

    /**
     * Key for {@link TemplateMap#getRootTemplate()}.
     */
    public static final String ROOT_TEMPLATE_ID = "root";

    private NodeFeatures() {
        // Only static
    }
}
