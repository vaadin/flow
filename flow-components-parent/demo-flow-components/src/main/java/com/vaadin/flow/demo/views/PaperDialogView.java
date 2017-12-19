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

import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.paper.button.GeneratedPaperButton;
import com.vaadin.flow.component.paper.dialog.GeneratedPaperDialog;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.ComponentDemo.DemoCategory;
import com.vaadin.flow.demo.DemoView;
import com.vaadin.flow.demo.MainLayout;
import com.vaadin.router.Route;

/**
 * View for {@link GeneratedPaperDialog} demo.
 */
@Route(value = "paper-dialog", layout = MainLayout.class)
@ComponentDemo(name = "Paper Dialog", category = DemoCategory.PAPER)
@HtmlImport("bower_components/neon-animation/neon-animations.html")
@HtmlImport("bower_components/neon-animation/web-animations.html")
public class PaperDialogView extends DemoView {

    private Div message;

    @Override
    protected void initView() {
        createPlainDialog();
        createModalDialog();
        createNestedDialogs();
        createDialogWithActions();
        createAnimatedDialog();
        createHelperMethodsCard();

        message = new Div();
        message.setId("dialogsMessage");
        add(message);
    }

    private void createPlainDialog() {
        // begin-source-example
        // source-example-heading: Plain dialog
        GeneratedPaperDialog<?> dialog = createDialog("Plain dialog",
                "Plain dialog with plain text. Click anywhere on the page to close.");

        GeneratedPaperButton<?> open = new GeneratedPaperButton<>(
                "Plain dialog");
        open.setRaised(true);
        open.addClickListener(evt -> addAndOpen(dialog));
        // end-source-example

        addCard("Plain dialog", open);
    }

    private void createModalDialog() {
        // begin-source-example
        // source-example-heading: Modal dialog
        GeneratedPaperDialog<?> dialog = createDialog("Modal dialog",
                "Modal dialog with plain text.");
        dialog.setModal(true);

        GeneratedPaperButton<?> close = new GeneratedPaperButton<>(
                "Close dialog");
        close.addClickListener(evt -> dialog.close());
        dialog.add(createDivForButton(close));

        GeneratedPaperButton<?> open = new GeneratedPaperButton<>(
                "Modal dialog");
        open.setRaised(true);
        open.addClickListener(evt -> addAndOpen(dialog));
        // end-source-example

        addCard("Modal dialog", open);
    }

    private void createNestedDialogs() {
        // begin-source-example
        // source-example-heading: Nested dialogs
        GeneratedPaperDialog<?> dialog = createDialog("Nested dialogs",
                "Click on the button to open the nested dialog. Click anywhere on the page to close.");

        GeneratedPaperDialog<?> second = createDialog("Second dialog",
                "This is the second dialog. Click anywhere on the page to close.");
        second.setId("second-dialog");

        GeneratedPaperButton<?> openSecond = new GeneratedPaperButton<>(
                "Open second dialog");
        openSecond.addClickListener(evt -> addAndOpen(second));
        dialog.add(createDivForButton(openSecond));

        GeneratedPaperButton<?> open = new GeneratedPaperButton<>(
                "Nested dialogs");
        open.setRaised(true);
        open.addClickListener(evt -> addAndOpen(dialog));
        // end-source-example

        addCard("Nested dialogs", open);
    }

    private void createDialogWithActions() {
        // begin-source-example
        // source-example-heading: Dialog with actions
        GeneratedPaperDialog<?> dialog = createDialog("Dialog with actions",
                "A dialog can have any number of actions. Click anywhere on the page to close.");

        GeneratedPaperButton<?> dummyAction = new GeneratedPaperButton<>(
                "Do nothing");

        GeneratedPaperButton<?> decline = new GeneratedPaperButton<>("Decline");
        decline.getElement().setAttribute("dialog-dismiss", true);

        GeneratedPaperButton<?> accept = new GeneratedPaperButton<>("Accept");
        accept.getElement().setAttribute("dialog-confirm", true);
        accept.getElement().setAttribute("autofocus", true);

        dialog.add(createDivForButton(dummyAction, decline, accept));

        GeneratedPaperButton<?> open = new GeneratedPaperButton<>(
                "Dialog with actions");
        open.setRaised(true);
        open.addClickListener(evt -> addAndOpen(dialog));
        // end-source-example

        addCard("Dialog with actions", open);
    }

    private void createAnimatedDialog() {
        // begin-source-example
        // source-example-heading: Animated dialog
        GeneratedPaperDialog<?> dialog = createDialog("Animated dialog",
                "Isn't it cool? Click anywhere on the page to close.");

        dialog.setEntryAnimation("scale-up-animation");
        dialog.setExitAnimation("scale-down-animation");
        dialog.setWithBackdrop(true);

        GeneratedPaperButton<?> open = new GeneratedPaperButton<>(
                "Animated dialog");
        open.setRaised(true);
        open.addClickListener(evt -> addAndOpen(dialog));
        // end-source-example

        addCard("Animated dialog", open);
    }

    private void createHelperMethodsCard() {
        addCard("Helper methods");
    }

    // begin-source-example
    // source-example-heading: Helper methods
    private GeneratedPaperDialog<?> createDialog(String title, String text) {
        GeneratedPaperDialog<?> dialog = new GeneratedPaperDialog<>();
        dialog.add(new H2(title));
        HtmlComponent p = new HtmlComponent("p");
        p.getElement().setText(text);
        dialog.add(p);
        dialog.addOpenedChangeListener(evt -> message
                .setText(dialog.isOpened() ? (title + " was opened")
                        : (title + " was closed")));
        return dialog;
    }

    private Div createDivForButton(GeneratedPaperButton<?>... buttons) {
        Div div = new Div(buttons);
        div.setClassName("buttons");
        return div;
    }

    private void addAndOpen(GeneratedPaperDialog<?> dialog) {
        if (dialog.getElement().getParent() == null) {
            getUI().ifPresent(ui -> ui.add(dialog));
        }
        dialog.open();
    }
    // end-source-example
}
