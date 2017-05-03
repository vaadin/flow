
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
import com.vaadin.flow.html.Button;
import com.vaadin.flow.html.Label;
import com.vaadin.ui.Component;

public class AttachExistingElementView extends AbstractDivView {

    @Override
    protected void onShow() {
        getPage().executeJavaScript(
                "$0.appendChild(document.createElement('span')); $0.appendChild(document.createElement('label'));"
                        + "$0.appendChild(document.createElement('div'));",
                getElement());

        Button attachLabel = new Button("Attach label", event -> getElement()
                .attachExistingElement("label", null, this::handleLabel));
        add(attachLabel);
    }

    private void handleLabel(Element label) {
        Label lbl = Component.from(label, Label.class);
        lbl.setText("Client side label");
        lbl.setId("label");
    }
}