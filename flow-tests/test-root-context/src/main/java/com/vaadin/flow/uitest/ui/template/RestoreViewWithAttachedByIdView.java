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

import com.vaadin.flow.model.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;
import com.vaadin.router.Route;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.html.Div;
import com.vaadin.ui.html.Label;
import com.vaadin.ui.html.NativeButton;
import com.vaadin.ui.polymertemplate.Id;
import com.vaadin.ui.polymertemplate.PolymerTemplate;

@Route(value = "com.vaadin.flow.uitest.ui.template.RestoreViewWithAttachedByIdView", layout = ViewTestLayout.class)
public class RestoreViewWithAttachedByIdView extends AbstractDivView {

    private TemplateWithInjectedId template;
    private Label label;

    private Component current;

    @Tag("template-with-injected-id")
    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/TemplateWithInjectedId.html")
    public static class TemplateWithInjectedId
            extends PolymerTemplate<TemplateModel> {

        @Id("ignore")
        private Div ignore;

        @Id("target")
        private Div target;

        public TemplateWithInjectedId() {
            target.setText("Server Side Text");
        }
    }

    public RestoreViewWithAttachedByIdView() {
        template = new TemplateWithInjectedId();
        label = new Label("Switched component");
        label.setId("info");
        template.setId("template");

        add(template);

        NativeButton button = new NativeButton("Switch components");
        current = template;
        button.addClickListener(event -> {
            remove(current);
            if (current == template) {
                current = label;
            } else {
                current = template;
            }
            add(current);
        });
        add(button);
    }
}
