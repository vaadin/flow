
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
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.Route;
import com.vaadin.ui.html.NativeButton;
import com.vaadin.ui.html.Div;
import com.vaadin.ui.html.H1;
import com.vaadin.ui.html.Label;
import com.vaadin.ui.Component;

@Route(value = "com.vaadin.flow.uitest.ui.AttachExistingElementView", layout = ViewTestLayout.class)
public class AttachExistingElementView extends AbstractDivView {

    private Label attachedLabel;

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
        setId("root-div");
        NativeButton attachLabel = new NativeButton("Attach label",
                event -> getElement().getStateProvider().attachExistingElement(
                        getElement().getNode(), "label", null,
                        this::handleLabel));
        attachLabel.setId("attach-label");
        add(attachLabel);
        NativeButton attachHeader = new NativeButton("Attach Header",
                event -> getElement().getStateProvider().attachExistingElement(
                        getElement().getNode(), "h1", null,
                        this::handleHeader));
        attachHeader.setId("attach-header");
        add(attachHeader);

        Div div = new Div();
        div.setId("element-with-shadow");
        ShadowRoot shadowRoot = div.getElement().attachShadow();

        NativeButton attachLabelInShadow = new NativeButton("Attach label in shadow",
                event -> shadowRoot.getStateProvider().attachExistingElement(
                        shadowRoot.getNode(), "label", null,
                        this::handleLabelInShadow));
        attachLabelInShadow.setId("attach-label-inshadow");
        add(attachLabelInShadow);

        NativeButton attachNonExistingElement = new NativeButton(
                "Attach non-existing element",
                event -> getElement().getStateProvider().attachExistingElement(
                        getElement().getNode(), "image", null,
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
        attachedLabel = Component.from(label, Label.class);
        attachedLabel.setText("Client side label");
        attachedLabel.setId("label");
        NativeButton attachPopulatedLabel = new NativeButton(
                "Attach the already attached label",
                event -> getElement().getStateProvider().attachExistingElement(
                        getElement().getNode(), "label", null,
                        this::handleAttachedLabel));
        attachPopulatedLabel.setId("attach-populated-label");
        add(attachPopulatedLabel);

        NativeButton removeSelf = new NativeButton("Remove myself on the server side",
                event -> event.getSource().getElement().removeFromParent());
        removeSelf.setId("remove-self");
        add(removeSelf);
    }

    private void handleAttachedLabel(Element label) {
        Element child = getElement().getChild(attachedLabel.getParent().get()
                .getElement().indexOfChild(attachedLabel.getElement()));
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
        lbl.setId("header");
    }

    private void handleLabelInShadow(Element label) {
        Label lbl = Component.from(label, Label.class);
        lbl.setText("Client side label in shadow root");
        lbl.setId("label-in-shadow");
    }

}
