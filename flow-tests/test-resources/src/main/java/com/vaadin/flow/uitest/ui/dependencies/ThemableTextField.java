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
import com.vaadin.flow.component.dependency.NpmPackage;

/**
 * Test-only field component used to verify Flow's per-component theming
 * ({@code theme/components/faux-text-field.css}). It is backed by the
 * {@code faux-text-field.js} custom element — a {@code ThemableMixin} element
 * whose tag deliberately matches no real Vaadin component — so the machinery
 * can be exercised without depending on a real Vaadin web component.
 * <p>
 * {@code @vaadin/vaadin-themable-mixin} is declared here rather than in a
 * shared module because every module that has {@code flow-test-resources} on
 * its class path picks up the {@code faux-text-field.js} import from this
 * class. Keeping the npm package on the same class guarantees the mixin is
 * installed wherever that import is generated.
 */
@JsModule("./faux-text-field.js")
@NpmPackage(value = "@vaadin/vaadin-themable-mixin", version = TestVersion.THEMABLE_MIXIN)
@Tag("faux-text-field")
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
