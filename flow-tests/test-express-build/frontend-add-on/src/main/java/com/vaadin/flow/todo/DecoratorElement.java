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
package com.vaadin.flow.todo;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

/**
 * An add-on element whose frontend source is TypeScript using experimental
 * decorators, which the bundle build has to transpile.
 */
@Tag(DecoratorElement.TAG)
@JsModule("./DecoratorElement.ts")
public class DecoratorElement extends Component {

    public static final String TAG = "decorator-element";

    /**
     * Sets the label rendered by the element, as a Lit reactive property.
     *
     * @param label
     *            the label to render
     */
    public void setLabel(String label) {
        getElement().setProperty("label", label);
    }
}
