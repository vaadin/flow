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

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.ClearNodeChildrenView", layout = ViewTestLayout.class)
@Tag("clear-node-children")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/ClearNodeChildren.html")
@JsModule("ClearNodeChildren.js")
public class ClearNodeChildrenView extends PolymerTemplate<TemplateModel>
        implements HasComponents, HasText {

    @Id
    private Div containerWithElementChildren;

    @Id
    private Div containerWithMixedChildren;

    @Id
    private Div containerWithClientSideChildren;

    @Id
    private Div containerWithSlottedChildren;

    @Id
    private Div containerWithContainer;

    @Id
    private Div nestedContainer;

    @Id
    private NativeButton addChildToContainer1;

    @Id
    private NativeButton addTextNodeToContainer1;

    @Id
    private NativeButton clearContainer1;

    @Id
    private NativeButton addChildToContainer2;

    @Id
    private NativeButton clearContainer2;

    @Id
    private NativeButton addChildToContainer3;

    @Id
    private NativeButton clearContainer3;

    @Id
    private NativeButton addChildToNestedContainer;

    @Id
    private NativeButton clearContainer4;

    @Id
    private NativeButton addChildToSlot;

    @Id
    private NativeButton clear;

    @Id
    private NativeButton setTextToContainer1;

    @Id
    private NativeButton setTextToContainer2;

    @Id
    private NativeButton setTextToContainer3;

    @Id
    private NativeButton setTextToContainer4;

    @Id
    private NativeButton setText;

    @Id
    private Div message;

    public ClearNodeChildrenView() {
        setId("root");
        addChildToContainer1.addClickListener(
                event -> addDivTo(containerWithElementChildren));
        addChildToContainer2.addClickListener(
                event -> addDivTo(containerWithMixedChildren));
        addChildToContainer3.addClickListener(
                event -> addDivTo(containerWithClientSideChildren));
        addChildToNestedContainer
                .addClickListener(event -> addDivTo(nestedContainer));
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
        clearContainer4.addClickListener(event -> clear(containerWithContainer,
                "containerWithContainer"));
        setTextToContainer1.addClickListener(event -> setTextTo(
                containerWithElementChildren, "containerWithElementChildren"));
        setTextToContainer2
                .addClickListener(event -> setTextTo(containerWithMixedChildren,
                        "containerWithMixedChildren"));
        setTextToContainer3.addClickListener(
                event -> setTextTo(containerWithClientSideChildren,
                        "containerWithClientSideChildren"));
        setTextToContainer4
                .addClickListener(event -> setTextTo(containerWithContainer,
                        "containerWithContainer"));
        clear.addClickListener(event -> clear(this, "root"));
        setText.addClickListener(event -> setTextTo(this, "root"));

        addTextNodeToContainer1.addClickListener(event -> {
            Element text = Element.createText("Text node");
            containerWithElementChildren.getElement().appendChild(text);
            message.setText(
                    "Added 'Text node' to div with id 'containerWithElementChildren'.");
        });
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

    private void setTextTo(HasText container, String id) {
        container.setText("Hello World");
        message.setText(message.getText() + "\nDiv '" + id + "' text set to '"
                + container.getText() + "'.");
    }

}
