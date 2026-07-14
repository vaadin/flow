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
package org.vaadin.example.addon;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

@Tag(AddonLitDecoratorComponent.TAG)
@JsModule("./AddonLitDecoratorComponent.ts")
public class AddonLitDecoratorComponent extends Component {

    public static final String TAG = "addon-lit-decorator-component";

    /**
     * Updates the Lit reactive {@code label} property. The value round-trips
     * through Lit's reactive accessor, so the rendered text only changes when
     * the decorator-defined property works.
     */
    public void setLabel(String value) {
        getElement().setProperty("label", value);
    }
}
