/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.ChildOrderView", layout = ViewTestLayout.class)
@Tag("child-order-template")
@JsModule("./ChildOrderTemplate.js")
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
