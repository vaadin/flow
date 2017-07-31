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
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.html.Label;
import com.vaadin.generated.paper.button.GeneratedPaperButton;
import com.vaadin.generated.paper.card.GeneratedPaperCard;
import com.vaadin.generated.paper.input.GeneratedPaperInput;
import com.vaadin.generated.paper.progress.GeneratedPaperProgress;
import com.vaadin.generated.paper.spinner.GeneratedPaperSpinner;

/**
 * Collection view that showcases multiple components on multiple cards.
 */
@ComponentDemo(href = "collection", name = "Collection")
public class Collection extends DemoView {

    @Override
    void initView() {
        GeneratedPaperProgress progress = new GeneratedPaperProgress();

        progress.getElement().setAttribute("indeterminate", "");
        progress.getElement().getStyle().set("width", "100%");

        add(createCard(), progress);

        GeneratedPaperButton button = new GeneratedPaperButton();
        button.getElement().setText("Button");
        button.getElement().setAttribute("raised", "");
        button.getElement().getStyle().set("backgroundColor", "white");

        GeneratedPaperInput input = new GeneratedPaperInput();

        GeneratedPaperSpinner paperSpinner = new GeneratedPaperSpinner();
        paperSpinner.getElement().setAttribute("active", "");

        Card card = addCard(button, input, paperSpinner);
    }

    @Override
    public void populateSources(SourceContent container) {
        container.add(new Label(
                "No sources here. Go to the wanted component for examples."));
    }

    private GeneratedPaperCard createCard() {
        GeneratedPaperCard card = new GeneratedPaperCard();

        Element cardContent = ElementFactory.createDiv();
        cardContent.setAttribute("class", "card-content");

        cardContent.appendChild(ElementFactory.createHeading2("Demo card"));

        Element cardActions = ElementFactory.createDiv();
        cardActions.setAttribute("class", "card-actions");
        Element buttons = ElementFactory.createDiv();
        buttons.setAttribute("class", "horizontal justified");

        cardActions.appendChild(buttons);

        card.getElement().appendChild(cardContent, cardActions);

        GeneratedPaperButton reserve = new GeneratedPaperButton();
        reserve.getElement().setText("Reserve");

        GeneratedPaperButton visit = new GeneratedPaperButton();
        visit.getElement().setText("Visit page");

        buttons.appendChild(visit.getElement())
                .appendChild(reserve.getElement());
        return card;
    }
}
