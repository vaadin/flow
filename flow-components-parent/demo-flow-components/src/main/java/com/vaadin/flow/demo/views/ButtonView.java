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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasClickListeners.ClickEvent;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.DemoView;
import com.vaadin.flow.demo.MainLayout;
import com.vaadin.router.Route;
import com.vaadin.ui.button.Button;
import com.vaadin.ui.icon.Icon;
import com.vaadin.ui.icon.VaadinIcons;

/**
 * View for {@link Button} demo.
 */
@Route(value = "vaadin-button", layout = MainLayout.class)
@HtmlImport("bower_components/vaadin-valo-theme/vaadin-button.html")
@ComponentDemo(name = "Button")
public class ButtonView extends DemoView {

    private Div message;

    @Override
    protected void initView() {
        createDefaultButton();
        createButtonsWithIcons();
        createImageButtonWithAutofocus();
        createImageButtonWithAccessibleLabel();
        createButtonsWithTabIndex();
        createDisabledButton();

        message = new Div();
        message.setId("buttonMessage");
        add(message);
    }

    private void createDefaultButton() {
        // begin-source-example
        // source-example-heading: Default button
        Button button = new Button("Vaadin button");

        button.addClickListener(this::showButtonClickedMessage);
        // end-source-example

        addCard("Default button", button);
        button.setId("default-button");
    }

    private void createButtonsWithIcons() {
        // begin-source-example
        // source-example-heading: Buttons with icons
        Button leftButton = new Button("Left",
                new Icon(VaadinIcons.ARROW_LEFT));

        Button rightButton = new Button("Right",
                new Icon(VaadinIcons.ARROW_RIGHT));
        rightButton.setIconAfterText(true);

        Button thumbsUpButton = new Button(new Icon(VaadinIcons.THUMBS_UP));
        // end-source-example

        leftButton.addClickListener(this::showButtonClickedMessage);
        rightButton.addClickListener(this::showButtonClickedMessage);
        thumbsUpButton.addClickListener(this::showButtonClickedMessage);

        addCard("Buttons with icons", leftButton, rightButton, thumbsUpButton);
        leftButton.setId("left-icon-button");
        rightButton.setId("right-icon-button");
        thumbsUpButton.setId("thumb-icon-button");
    }

    private void createImageButtonWithAutofocus() {
        // begin-source-example
        // source-example-heading: Button with image and autofocus
        Button button = new Button(
                new Image("img/vaadin-logo.svg", "Vaadin logo"));
        button.setAutofocus(true);

        button.addClickListener(this::showButtonClickedMessage);
        // end-source-example

        addCard("Button with image and autofocus", button);
        button.setId("image-button");
    }

    private void createImageButtonWithAccessibleLabel() {
        // begin-source-example
        // source-example-heading: Button with ARIA label
        Button button = new Button("Accessible");
        button.getElement().setAttribute("aria-label", "Click me");

        button.addClickListener(this::showButtonClickedMessage);
        // end-source-example

        addCard("Button with ARIA label", button);
        button.setId("accessible-button");
    }

    private void createButtonsWithTabIndex() {
        // begin-source-example
        // source-example-heading: Buttons with custom tabindex
        Button button1 = new Button("1");
        button1.setTabIndex(1);
        button1.addClickListener(this::showButtonClickedMessage);

        Button button2 = new Button("2");
        button2.setTabIndex(2);
        button2.addClickListener(this::showButtonClickedMessage);

        Button button3 = new Button("3");
        button3.setTabIndex(3);
        button3.addClickListener(this::showButtonClickedMessage);
        // end-source-example

        addCard("Buttons with custom tabindex", button3, button2, button1);
        button1.setId("button-tabindex-1");
        button2.setId("button-tabindex-2");
        button3.setId("button-tabindex-3");
    }

    private void createDisabledButton() {
        // begin-source-example
        // source-example-heading: Disabled button
        Button button = new Button("Disabled");
        button.setDisabled(true);
        // end-source-example

        addCard("Disabled button", button);
        button.addClickListener(evt -> message.setText("Button "
                + evt.getSource().getText()
                + " was clicked, but the button is disabled and this shouldn't happen!"));
        button.setId("disabled-button");
    }

    private void showButtonClickedMessage(ClickEvent<Button> evt) {
        Button source = evt.getSource();
        source.getParent()
                .ifPresent(parent -> parent.getElement().insertChild(
                        parent.getElement().getChildCount() - 2,
                        message.getElement()));

        String text = source.getText();
        if (text.isEmpty() && containsChild(source, "img")) {
            text = "with image";
        } else if (text.isEmpty() && containsChild(source, "iron-icon")) {
            text = "thumbs up";
        }

        message.setText("Button " + text + " was clicked.");
    }

    private boolean containsChild(Component parent, String tagName) {
        return parent.getElement().getChildren()
                .anyMatch(element -> element.getTag().equals(tagName));
    }
}
