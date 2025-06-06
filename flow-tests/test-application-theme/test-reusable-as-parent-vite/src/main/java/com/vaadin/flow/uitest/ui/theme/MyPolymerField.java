/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.theme;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

import com.vaadin.flow.uitest.ui.dependencies.TestVersion;

/**
 * Polymer version of vaadin text field for testing component theming.
 */
@JsModule("@vaadin/text-field/vaadin-text-field.js")
@Tag("vaadin-text-field")
@NpmPackage(value = "@vaadin/text-field", version = TestVersion.VAADIN)
public class MyPolymerField extends Component {

    /**
     * Set the component id.
     *
     * @param id
     *            value to set
     * @return this component
     */
    public Component withId(String id) {
        setId(id);
        return this;
    }
}
