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
package com.vaadin.flow.uitest.ui.template.imports;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.imports.LazyWidgetView", layout = ViewTestLayout.class)
@Tag("x-lazy-widget")
@HtmlImport(value = "frontend://com/vaadin/flow/uitest/ui/template/imports/x-lazy-widget.html", loadMode = LoadMode.LAZY)
@JsModule("x-lazy-widget.js")
public class LazyWidgetView extends PolymerTemplate<LazyWidgetView.Model> {
    static final String GREETINGS_TEMPLATE = "Greetings from server, %s!";

    public LazyWidgetView() {
        setId("template");
    }

    public interface Model extends TemplateModel {
        void setHasGreeting(boolean hasGreeting);

        void setGreeting(String greeting);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        getModel().setHasGreeting(false);
        getModel().setGreeting("");
    }

    @ClientCallable
    void greet(String name) {
        getModel().setGreeting(String.format(GREETINGS_TEMPLATE, name));
        getModel().setHasGreeting(name != null && !name.isEmpty());
    }
}
