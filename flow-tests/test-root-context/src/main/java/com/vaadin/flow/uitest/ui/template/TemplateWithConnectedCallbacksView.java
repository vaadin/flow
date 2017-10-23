/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;
import com.vaadin.router.Route;
import com.vaadin.ui.html.NativeButton;

@Route(value = "com.vaadin.flow.uitest.ui.template.TemplateWithConnectedCallbacksView", layout = ViewTestLayout.class)
public class TemplateWithConnectedCallbacksView extends AbstractDivView {

    private TemplateWithConnectedCallbacks component;
    private NativeButton button;

    public TemplateWithConnectedCallbacksView() {
        button = new NativeButton("Toggle template", evt -> {
            if (component == null) {
                addNewTemplate();
            } else {
                remove(component);
                component = null;
            }
        });
        button.setId("toggle-button");
        add(button);
        addNewTemplate();
    }

    private void addNewTemplate() {
        component = new TemplateWithConnectedCallbacks();
        add(component);
    }
}
