/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

/**
 * Test-only stand-in for {@code vaadin-text-field} used to verify Flow's
 * per-component theming ({@code theme/components/vaadin-text-field.css}). It is
 * backed by the faux {@code themable-input.js} custom element — a
 * {@code ThemableMixin} element registered under the {@code vaadin-text-field}
 * tag — so the machinery can be exercised without depending on the real Vaadin
 * text field web component. The mixin itself is provided transitively via
 * {@code flow-test-lumo}.
 */
@JsModule("./themable-input.js")
@Tag("vaadin-text-field")
public class ThemableTextField extends Component {

    /**
     * Set the component id.
     *
     * @param id
     *            value to set
     */
    public void withId(String id) {
        setId(id);
    }
}
