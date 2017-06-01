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
package com.vaadin.flow.uitest.ui.template.imports;

import com.vaadin.annotations.ClientDelegate;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.router.View;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.template.model.TemplateModel;
import com.vaadin.shared.ui.LoadMode;
import com.vaadin.ui.AttachEvent;

@Tag("x-lazy-widget")
@HtmlImport(value = "/com/vaadin/flow/uitest/ui/template/imports/x-lazy-widget.html", loadMode = LoadMode.LAZY)
public class LazyWidgetView extends PolymerTemplate<LazyWidgetView.Model> implements View {
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

    @ClientDelegate
    void greet(String name) {
        getModel().setGreeting(String.format(GREETINGS_TEMPLATE, name));
        getModel().setHasGreeting(name != null && !name.isEmpty());
    }
}
