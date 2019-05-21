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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.ChildOrderView", layout = ViewTestLayout.class)
@Tag("child-order-template")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/ChildOrderTemplate.html")
@JsModule("ChildOrderTemplate.js")
public class ChildOrderView extends PolymerTemplate<TemplateModel> {

    @Id
    private Div containerWithElement;

    @Id
    private Div containerWithText;

    @Id
    private Div containerWithElementAddedOnConstructor;

    @Id
    private NativeButton addChildToContainer1;

    @Id
    private NativeButton prependChildToContainer1;

    @Id
    private NativeButton removeChildFromContainer1;

    @Id
    private NativeButton addChildToContainer2;

    @Id
    private NativeButton prependChildToContainer2;

    @Id
    private NativeButton removeChildFromContainer2;

    public ChildOrderView() {
        setId("root");

        Div childOnConstructor1 = new Div();
        childOnConstructor1.setText(
                "Server child " + (containerWithElementAddedOnConstructor
                        .getElement().getChildCount() + 1));
        containerWithElementAddedOnConstructor.add(childOnConstructor1);

        Div childOnConstructor2 = new Div();
        childOnConstructor2.setText(
                "Server child " + (containerWithElementAddedOnConstructor
                        .getElement().getChildCount() + 1));
        containerWithElementAddedOnConstructor.add(childOnConstructor2);

        addChildToContainer1.addClickListener(event -> {
            Div div = new Div();
            div.setText("Server child "
                    + (containerWithElement.getElement().getChildCount() + 1));
            containerWithElement.add(div);
        });

        prependChildToContainer1.addClickListener(event -> {
            Div div = new Div();
            div.setText("Server child "
                    + (containerWithElement.getElement().getChildCount() + 1));
            containerWithElement.getElement().insertChild(0, div.getElement());
        });

        removeChildFromContainer1.addClickListener(event -> {
            if (containerWithElement.getElement().getChildCount() > 0) {
                containerWithElement.getElement().removeChild(
                        containerWithElement.getElement().getChildCount() - 1);
            }
        });

        addChildToContainer2.addClickListener(event -> {
            Element text = Element.createText("\nServer text "
                    + (containerWithText.getElement().getChildCount() + 1));
            containerWithText.getElement().appendChild(text);
        });

        prependChildToContainer2.addClickListener(event -> {
            Element text = Element.createText("\nServer text "
                    + (containerWithText.getElement().getChildCount() + 1));
            containerWithText.getElement().insertChild(0, text);
        });

        removeChildFromContainer2.addClickListener(event -> {
            if (containerWithText.getElement().getChildCount() > 0) {
                containerWithText.getElement().removeChild(
                        containerWithText.getElement().getChildCount() - 1);
            }
        });
    }
}
