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

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.model.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.ParentLayout;
import com.vaadin.router.Route;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.polymertemplate.EventHandler;
import com.vaadin.ui.polymertemplate.Id;
import com.vaadin.ui.polymertemplate.PolymerTemplate;

@Route(value = "com.vaadin.flow.uitest.ui.template.AttachExistingDomElementByIdView", layout = ViewTestLayout.class)
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/AttachExistingDomElementById.html")
@Tag("existing-dom-element")
public class AttachExistingDomElementByIdView
        extends PolymerTemplate<TemplateModel> {

    @Id("input")
    private Element input;

    @Id("label")
    private Element label;

    public AttachExistingDomElementByIdView() {
        setId("template");

        input.setProperty("placeholder", "Foo");
        label.setText("bar");
        input.synchronizeProperty("value", "change");
    }

    @EventHandler
    private void clear() {
        label.setText("default");
        input.setProperty("value", "");
    }

    @EventHandler
    private void valueChange() {
        label.setText("Text from input " + input.getProperty("value"));
    }

}
