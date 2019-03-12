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
package com.vaadin.flow.component.webcomponent;

import java.io.Serializable;

import com.vaadin.flow.component.Component;

import elemental.json.JsonValue;

/**
 * WebComponent to be configured by {@link InstanceConfigurator}
 */
public interface IWebComponent<C extends Component> extends Serializable {
    void fireEvent(String eventName);

    void fireEvent(String eventName, JsonValue objectData);

    void fireEvent(String eventName, JsonValue objectData,
                   EventOptions options);

    <P> void setProperty(
            PropertyConfiguration<C, P> propertyConfiguration, P value);

    <P> P getProperty(PropertyConfiguration<C, P> propertyConfiguration);
}