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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.paper.button.GeneratedPaperButton;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.ComponentDemo.DemoCategory;
import com.vaadin.flow.demo.DemoView;
import com.vaadin.flow.demo.MainLayout;
import com.vaadin.router.Route;

/**
 * View for {@link GeneratedPaperButton} demo.
 */
@Route(value = "paper-button", layout = MainLayout.class)
@ComponentDemo(name = "Paper Button", category = DemoCategory.PAPER)
public class PaperButtonView extends DemoView {

    @Override
    public void initView() {
        createRaisedButton();
        createLinkButton();
        createToggleButton();
        createDisabledButton();
    }

    private void createLinkButton() {
        Div message = new Div();

        // begin-source-example
        // source-example-heading: Link button
        GeneratedPaperButton<?> link = new GeneratedPaperButton<>("Link");
        link.setRaised(false);
        link.setNoink(true);
        link.getStyle().set("backgroundColor", "white");
        link.addClickListener(
                evt -> message.setText("Button LINK was clicked."));
        // end-source-example

        link.setId("link-button");
        message.setId("link-button-message");
        addCard("Link button", link, message);
    }

    private void createRaisedButton() {
        Div message = new Div();

        // begin-source-example
        // source-example-heading: Raised button
        GeneratedPaperButton<?> raised = new GeneratedPaperButton<>("Raised");
        raised.setRaised(true);
        raised.getStyle().set("backgroundColor", "white");
        raised.addClickListener(
                evt -> message.setText("Button RAISED was clicked."));
        // end-source-example

        raised.setId("raised-button");
        message.setId("raised-button-message");
        addCard("Raised button", raised, message);
    }

    private void createToggleButton() {
        Div message = new Div();

        // begin-source-example
        // source-example-heading: Toggle button
        GeneratedPaperButton<?> toggles = new GeneratedPaperButton<>("Toggles");
        toggles.setRaised(true);
        toggles.setToggles(true);
        toggles.getStyle().set("backgroundColor", "white");
        toggles.addClickListener(
                evt -> message.setText("Button TOGGLES was clicked."));
        // end-source-example

        toggles.setId("toggle-button");
        message.setId("toggle-button-message");
        addCard("Toggle button", toggles, message);
    }

    private void createDisabledButton() {
        // begin-source-example
        // source-example-heading: Disabled button
        GeneratedPaperButton<?> disabled = new GeneratedPaperButton<>(
                "Disabled");
        disabled.setRaised(false);
        disabled.setDisabled(true);
        disabled.getStyle().set("backgroundColor", "white");
        // end-source-example

        disabled.setId("disabled-button");
        addCard("Disabled button", disabled);
    }

}
