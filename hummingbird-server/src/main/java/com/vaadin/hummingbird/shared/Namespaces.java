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

import com.vaadin.hummingbird.namespace.ClassListNamespace;
import com.vaadin.hummingbird.namespace.ComponentMappingNamespace;
import com.vaadin.hummingbird.namespace.DependencyListNamespace;
import com.vaadin.hummingbird.namespace.ElementAttributeNamespace;
import com.vaadin.hummingbird.namespace.ElementChildrenNamespace;
import com.vaadin.hummingbird.namespace.ElementDataNamespace;
import com.vaadin.hummingbird.namespace.ElementListenersNamespace;
import com.vaadin.hummingbird.namespace.ElementPropertyNamespace;
import com.vaadin.hummingbird.namespace.ElementStylePropertyNamespace;
import com.vaadin.hummingbird.namespace.PollConfigurationNamespace;
import com.vaadin.hummingbird.namespace.PushConfigurationMap;
import com.vaadin.hummingbird.namespace.PushConfigurationMap.PushConfigurationParametersMap;
import com.vaadin.hummingbird.namespace.ReconnectDialogConfigurationNamespace;
import com.vaadin.hummingbird.namespace.SynchronizedPropertiesNamespace;
import com.vaadin.hummingbird.namespace.SynchronizedPropertyEventsNamespace;
import com.vaadin.hummingbird.namespace.TemplateNamespace;
import com.vaadin.hummingbird.namespace.TextNodeNamespace;

/**
 * Registry of namespace id numbers and map keys shared between server and
 * client.
 *
 * @since
 * @author Vaadin Ltd
 */
public class Namespaces {
    /**
     * Id for {@link ElementDataNamespace}.
     */
    public static final int ELEMENT_DATA = 0;

    /**
     * Id for {@link ElementPropertyNamespace}.
     */
    public static final int ELEMENT_PROPERTIES = 1;

    /**
     * Id for {@link ElementChildrenNamespace}.
     */
    public static final int ELEMENT_CHILDREN = 2;

    /**
     * Id for {@link ElementAttributeNamespace}.
     */
    public static final int ELEMENT_ATTRIBUTES = 3;

    /**
     * Id for {@link ElementListenersNamespace}.
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
     * Id for {@link TextNodeNamespace}.
     */
    public static final int TEXT_NODE = 7;
    /**
     * Id for {@link PollConfigurationNamespace}.
     */
    public static final int POLL_CONFIGURATION = 8;
    /**
     * Id for {@link ReconnectDialogConfigurationNamespace}.
     */
    public static final int RECONNECT_DIALOG_CONFIGURATION = 9;
    /**
     * Id for {@link ReconnectDialogConfigurationNamespace}.
     */
    public static final int LOADING_INDICATOR_CONFIGURATION = 10;
    /**
     * Id for {@link ClassListNamespace}.
     */
    public static final int CLASS_LIST = 11;
    /**
     * Id for {@link DependencyListNamespace}.
     */
    public static final int DEPENDENCY_LIST = 12;
    /**
     * Id for {@link ElementStylePropertyNamespace}.
     */
    public static final int ELEMENT_STYLE_PROPERTIES = 13;
    /**
     * Id for {@link SynchronizedPropertiesNamespace}.
     */
    public static final int SYNCHRONIZED_PROPERTIES = 14;
    /**
     * Id for {@link SynchronizedPropertyEventsNamespace}.
     */
    public static final int SYNCHRONIZED_PROPERTY_EVENTS = 15;
    /**
     * Id for {@link ComponentMappingNamespace}.
     */
    public static final int COMPONENT_MAPPING = 16;
    /**
     * Id for {@link TemplateNamespace}.
     */
    public static final int TEMPLATE = 17;

    /**
     * Key for {@link ElementDataNamespace#getTag()}.
     */
    public static final String TAG = "tag";

    /**
     * Key for {@link TextNodeNamespace#getText()}.
     */
    public static final String TEXT = "text";

    /**
     * Key for {@link TemplateNamespace#getRootTemplate()}.
     */
    public static final String ROOT_TEMPLATE_ID = "root";

    private Namespaces() {
        // Only static
    }
}
