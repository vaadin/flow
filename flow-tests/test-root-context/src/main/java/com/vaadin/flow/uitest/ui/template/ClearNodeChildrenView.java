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

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.ClearNodeChildrenView", layout = ViewTestLayout.class)
@Tag("clear-node-children")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/ClearNodeChildren.html")
public class ClearNodeChildrenView extends PolymerTemplate<TemplateModel>
        implements HasComponents {

    @Id("containerWithElementChildren")
    private Div containerWithElementChildren;

    @Id("containerWithMixedChildren")
    private Div containerWithMixedChildren;

    @Id("containerWithClientSideChildren")
    private Div containerWithClientSideChildren;

    @Id("containerWithSlottedChildren")
    private Div containerWithSlottedChildren;

    @Id("addChildToContainer1")
    private NativeButton addChildToContainer1;

    @Id("clearContainer1")
    private NativeButton clearContainer1;

    @Id("addChildToContainer2")
    private NativeButton addChildToContainer2;

    @Id("clearContainer2")
    private NativeButton clearContainer2;

    @Id("addChildToContainer3")
    private NativeButton addChildToContainer3;

    @Id("clearContainer3")
    private NativeButton clearContainer3;

    @Id("addChildToSlot")
    private NativeButton addChildToSlot;

    @Id("clear")
    private NativeButton clear;

    @Id("message")
    private Div message;

    public ClearNodeChildrenView() {
        setId("root");
        addChildToContainer1.addClickListener(
                event -> addDivTo(containerWithElementChildren));
        addChildToContainer2.addClickListener(
                event -> addDivTo(containerWithMixedChildren));
        addChildToContainer3.addClickListener(
                event -> addDivTo(containerWithClientSideChildren));
        addChildToSlot.addClickListener(event -> addDivTo(this));
        clearContainer1
                .addClickListener(event -> clear(containerWithElementChildren,
                        "containerWithElementChildren"));
        clearContainer2
                .addClickListener(event -> clear(containerWithMixedChildren,
                        "containerWithMixedChildren"));
        clearContainer3.addClickListener(
                event -> clear(containerWithClientSideChildren,
                        "containerWithClientSideChildren"));
        clear.addClickListener(event -> clear(this, "root"));
    }

    private void addDivTo(HasComponents container) {
        Div div = new Div();
        div.setText(
                "Server div " + (container.getElement().getChildCount() + 1));
        div.addAttachListener(evt -> message.setText(
                message.getText() + "\nDiv '" + div.getText() + "' attached."));
        div.addDetachListener(evt -> message.setText(
                message.getText() + "\nDiv '" + div.getText() + "' detached."));
        container.add(div);
    }

    private void clear(HasComponents container, String id) {
        container.removeAll();
        message.setText(message.getText() + "\nDiv '" + id + "' cleared.");
    }

}
