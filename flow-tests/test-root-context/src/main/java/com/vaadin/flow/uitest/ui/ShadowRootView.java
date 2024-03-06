/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ShadowRootView", layout = ViewTestLayout.class)
public class ShadowRootView extends AbstractDivView implements HasDynamicTitle {

    @Override
    protected void onShow() {
        Div div = new Div();
        div.getElement().setAttribute("id", "test-element");
        add(div);

        ShadowRoot shadowRoot = div.getElement().attachShadow();
        Element shadowDiv = ElementFactory.createDiv();
        shadowDiv.setText("Div inside shadow DOM");
        shadowDiv.setAttribute("id", "shadow-div");
        shadowRoot.appendChild(shadowDiv);
        Element shadowLabel = ElementFactory
                .createLabel("Label inside shadow DOM");
        shadowLabel.setAttribute("id", "shadow-label");
        shadowRoot.appendChild(shadowLabel);

        NativeButton removeChild = createButton(
                "Remove the last child from the shadow root", "remove",
                event -> shadowRoot.removeChild(shadowLabel));
        add(removeChild);
    }

    @Override
    public String getPageTitle() {
        return "Shadow root view";
    }
}
