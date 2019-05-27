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
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Tag("multiple-props-mutation")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/MultiplePropsMutation.html")
@JsModule("MultiplePropsMutation.js")
@Route(value = "com.vaadin.flow.uitest.ui.template.MutationSeveralSyncedPropsView", layout = ViewTestLayout.class)
public class MutationSeveralSyncedPropsView
        extends PolymerTemplate<TemplateModel> {

    public MutationSeveralSyncedPropsView() {
        getElement().synchronizeProperty("name", "name-changed");
        getElement().synchronizeProperty("message", "message-changed");

        getElement().setProperty("name", "foo");
        getElement().setProperty("message", "msg");

        setId("template");

        NativeButton button = AbstractDivView.createButton(
                "Update two synchronized properties simultaneously", "update",
                event -> {
                    getElement().setProperty("name", "bar");
                    getElement().setProperty("message", "baz");
                });
        getElement().appendChild(button.getElement());
    }
}
