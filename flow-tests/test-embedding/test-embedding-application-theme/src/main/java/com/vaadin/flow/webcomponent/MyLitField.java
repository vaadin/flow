/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

/**
 * LIT version of vaadin radio button for testing component theming.
 */
@JsModule("@vaadin/vaadin-radio-button/vaadin-radio-button.ts")
@Tag("vaadin-radio-button")
@NpmPackage(value = "@vaadin/vaadin-radio-button", version = "2.0.0-alpha1")
public class MyLitField extends Component {

    /**
     * Set the component id.
     *
     * @param id
     *     value to set
     * @return this component
     */
    public Component withId(String id) {
        setId(id);
        return this;
    }
}
