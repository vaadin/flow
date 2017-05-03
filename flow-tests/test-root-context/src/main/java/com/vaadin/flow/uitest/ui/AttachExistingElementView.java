
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

import com.vaadin.flow.dom.ChildElementConsumer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Node;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.html.Button;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.html.H1;
import com.vaadin.flow.html.Label;
import com.vaadin.ui.Component;

public class AttachExistingElementView extends AbstractDivView {

    private class NonExistingElementCallback implements ChildElementConsumer {

        @Override
        public void accept(Element child) {
            throw new IllegalStateException();
        }

        @Override
        public void onError(Node<?> parent, String tag,
                Element previousSibling) {
            Div div = new Div();
            div.setText("Non-exisint element error");
            div.setId("non-existing-element");
            add(div);
        }

    }

    @Override
    protected void onShow() {
        Button attachLabel = new Button("Attach label", event -> getElement()
                .attachExistingElement("label", null, this::handleLabel));
        attachLabel.setId("attach-label");
        add(attachLabel);
        Button attachHeader = new Button("Attach Header", event -> getElement()
                .attachExistingElement("h1", null, this::handleHeader));
        attachHeader.setId("attach-header");
        add(attachHeader);

        Div div = new Div();
        div.setId("element-with-shadow");
        ShadowRoot shadowRoot = div.getElement().attachShadow();

        Button attachLabelInShadow = new Button("Attach label in shadow",
                event -> shadowRoot.attachExistingElement("label", null,
                        this::handleLabelInShadow));
        attachLabelInShadow.setId("attach-label-inshadow");
        add(attachLabelInShadow);

        Button attachNonExistingElement = new Button(
                "Attach non-existing element",
                event -> getElement().attachExistingElement("image", null,
                        new NonExistingElementCallback()));
        attachNonExistingElement.setId("non-existing-element");
        add(attachNonExistingElement);

        add(div);
        getPage().executeJavaScript(
                "$0.appendChild(document.createElement('label'));", shadowRoot);

        getPage().executeJavaScript(
                "$0.appendChild(document.createElement('span')); $0.appendChild(document.createElement('label'));"
                        + "$0.appendChild(document.createElement('h1'));",
                getElement());
    }

    private void handleLabel(Element label) {
        Label lbl = Component.from(label, Label.class);
        lbl.setText("Client side label");
        lbl.setId("label");
        Button attachPopulatedLabel = new Button("Attach the attached label",
                event -> getElement().attachExistingElement("label", null,
                        this::handleAttachedLabel));
        attachPopulatedLabel.setId("attach-populated-label");
        add(attachPopulatedLabel);
    }

    private void handleAttachedLabel(Element label) {
        Element child = getElement().getChild(5);
        // child is already populated label. The <code>label</code> should be
        // the same element
        if (child.equals(label)) {
            child.getClassList().add("already-populated");
        } else {
            child.getClassList().add("new-label");
        }
    }

    private void handleHeader(Element header) {
        H1 lbl = Component.from(header, H1.class);
        lbl.setText("Client side header");
        lbl.setId("div");
    }

    private void handleLabelInShadow(Element label) {
        Label lbl = Component.from(label, Label.class);
        lbl.setText("Client side label in shadow root");
        lbl.setId("label-in-shadow");
    }

}