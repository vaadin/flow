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
package com.vaadin.flow.demo.views;

import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.SourceContent;
import com.vaadin.flow.html.Div;
import com.vaadin.generated.paper.button.GeneratedPaperButton;

/**
 * View for {@link GeneratedPaperButton} demo.
 */
@ComponentDemo(name = "Paper Button", href = "paper-button")
public class PaperButtonView extends DemoView {

    private Div message;

    @Override
    public void initView() {
        add(createButton("Link").setRaised(false).setNoink(true));
        add(createButton("Raised").setRaised(true));
        add(createButton("Toggles").setRaised(true).setToggles(true));
        add(createButton("Disabled").setRaised(false).setDisabled(true));

        message = new Div();
        message.setId("buttonsMessage");
        add(message);
    }

    private GeneratedPaperButton createButton(String text) {
        GeneratedPaperButton button = new GeneratedPaperButton(text);
        button.getStyle().set("backgroundColor", "white");
        button.addClickListener(evt -> {
            message.setText("Button " + evt.getSource().getText().toUpperCase()
                    + " was clicked.");
        });
        return button;
    }

    @Override
    public void populateSources(SourceContent container) {
        container.addCode("PaperButton button = new PaperButton();\n"
                + "button.setRaised(true);\n" + "button.setText(\"Button\");\n"
                + "layoutComponent.add(button);");
    }
}
