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

import com.vaadin.components.paper.button.PaperButton;
import com.vaadin.components.paper.card.PaperCard;
import com.vaadin.components.paper.input.PaperInput;
import com.vaadin.components.paper.progress.PaperProgress;
import com.vaadin.components.paper.spinner.PaperSpinner;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;

/**
 * Collection view that showcases multiple components on multiple cards.
 */
@ComponentDemo(href = "collection", name = "Collection")
public class Collection extends DemoView {

    @Override
    void initView() {
        PaperProgress progress = new PaperProgress();

        progress.getElement().setAttribute("indeterminate", "");
        progress.getElement().getStyle().set("width", "100%");

        add(createCard(), progress);

        PaperButton button = new PaperButton();
        button.getElement().setText("Button");
        button.getElement().setAttribute("raised", "");
        button.getElement().getStyle().set("backgroundColor", "white");

        PaperInput input = new PaperInput();

        PaperSpinner paperSpinner = new PaperSpinner();
        paperSpinner.getElement().setAttribute("active", "");

        Card card = addCard(button, input, paperSpinner);
    }

    private PaperCard createCard() {
        PaperCard card = new PaperCard();

        Element cardContent = ElementFactory.createDiv();
        cardContent.setAttribute("class", "card-content");

        cardContent.appendChild(ElementFactory.createHeading2("Demo card"));

        Element cardActions = ElementFactory.createDiv();
        cardActions.setAttribute("class", "card-actions");
        Element buttons = ElementFactory.createDiv();
        buttons.setAttribute("class", "horizontal justified");

        cardActions.appendChild(buttons);

        card.getElement().appendChild(cardContent, cardActions);

        PaperButton reserve = new PaperButton();
        reserve.getElement().setText("Reserve");

        PaperButton visit = new PaperButton();
        visit.getElement().setText("Visit page");

        buttons.appendChild(visit.getElement())
                .appendChild(reserve.getElement());
        return card;
    }
}
