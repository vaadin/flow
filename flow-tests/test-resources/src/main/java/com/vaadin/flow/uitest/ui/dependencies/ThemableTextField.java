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
 * Test-only themable component that imitates the shadow-DOM shape of
 * vaadin-text-field so theme-related ITs can verify Flow's per-component CSS
 * machinery without pulling in an actual Vaadin web component.
 */
@JsModule("./themable-input.js")
@Tag("themable-input")
public class ThemableTextField extends Component {

    public void withId(String id) {
        setId(id);
    }
}
