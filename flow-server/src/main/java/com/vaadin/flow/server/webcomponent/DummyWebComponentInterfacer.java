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

package com.vaadin.flow.server.webcomponent;

import java.io.Serializable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.webcomponent.EventOptions;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.component.webcomponent.PropertyConfiguration;

import elemental.json.JsonValue;

class DummyWebComponentInterfacer<C extends Component> implements WebComponent<C> {

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
    public <P extends Serializable> void setProperty(PropertyConfiguration<C, P> propertyConfiguration, P value) {

    }

    @Override
    public <P extends Serializable> P getProperty(PropertyConfiguration<C, P> propertyConfiguration) {
        return null;
    }

}
