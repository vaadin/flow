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
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.template.TemplateInTemplateView", layout = ViewTestLayout.class)
public class TemplateInTemplateView extends AbstractDivView {

    @Tag("parent-template")
    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/ParentTemplate.html")
    @Uses(ChildTemplate.class)
    @JsModule("ParentTemplate.js")
    public static class ParentTemplate extends PolymerTemplate<Message> {

    }

    @Tag("child-template")
    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/ChildTemplate.html")
    @JsModule("ChildTemplate.js")
    public static class ChildTemplate extends PolymerTemplate<Message> {

        @EventHandler
        private void handleClick() {
            getModel().setText("foo");
            Div div = new Div();
            div.setText("Click is handled by child template");
            div.setId("click-handler");
            getUI().get().add(div);
        }
    }

    @Override
    protected void onShow() {
        ParentTemplate template = new ParentTemplate();
        template.setId("template");
        add(template);
    }
}
