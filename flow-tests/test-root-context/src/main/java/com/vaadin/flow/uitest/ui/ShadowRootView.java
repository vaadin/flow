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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.ui.html.NativeButton;
import com.vaadin.ui.html.Div;
import com.vaadin.flow.router.LocationChangeEvent;

/**
 * @author Vaadin Ltd
 *
 */
public class ShadowRootView extends AbstractDivView {

    @Override
    public String getTitle(LocationChangeEvent locationChangeEvent) {
        return "Shadow root view";
    }

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

        NativeButton removeChild = new NativeButton(
                "Remove the last child from the shadow root",
                event -> shadowRoot.removeChild(shadowLabel));
        removeChild.setId("remove");
        add(removeChild);
    }
}
