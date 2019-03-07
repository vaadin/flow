/*
 * Copyright 2000-2019 Vaadin Ltd.
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

package com.vaadin.flow.server.webcomponent;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.webcomponent.EventOptions;
import com.vaadin.flow.component.webcomponent.IWebComponent;
import com.vaadin.flow.component.webcomponent.PropertyConfiguration;

import elemental.json.JsonValue;

public class DummyWebComponentInterfacer<C extends Component> implements IWebComponent<C> {

    @Override
    public void fireEvent(String eventName) {

    }

    @Override
    public void fireEvent(String eventName, JsonValue objectData) {

    }

    @Override
    public void fireEvent(String eventName, JsonValue objectData, EventOptions options) {

    }

    @Override
    public <P> void setProperty(PropertyConfiguration<C, P> propertyConfiguration, P value) {

    }

    @Override
    public <P> P getProperty(PropertyConfiguration<C, P> propertyConfiguration) {
        return null;
    }
}
