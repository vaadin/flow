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
package com.vaadin.flow.frontend;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.todo.DecoratorElement;

/**
 * Uses an add-on element whose TypeScript source relies on experimental
 * decorators, so that the dev bundle build has to transpile it.
 */
@Route("com.vaadin.flow.frontend.AddonDecoratorView")
public class AddonDecoratorView extends Div {

    static final String UPDATE_BUTTON_ID = "update-decorator-label";
    static final String UPDATED_LABEL = "Updated";

    public AddonDecoratorView() {
        DecoratorElement element = new DecoratorElement();
        add(element);

        NativeButton update = new NativeButton("Update label",
                event -> element.setLabel(UPDATED_LABEL));
        update.setId(UPDATE_BUTTON_ID);
        add(update);
    }
}
