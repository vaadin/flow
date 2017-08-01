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
import com.vaadin.generated.paper.button.GeneratedPaperButton;
import com.vaadin.generated.paper.card.GeneratedPaperCard;

/**
 * View for {@link GeneratedPaperCard} demo.
 */
@ComponentDemo(name = "Paper Card", href = "paper-card")
public class PaperCardView extends DemoView {

    @Override
    public void initView() {
        GeneratedPaperCard card = new GeneratedPaperCard();
        card.setImage(
                "https://vaadin.com/image/image_gallery?uuid=42c717c0-b63b-4c39-8ee8-3a14b6f477f6&groupId=10187&t=1359960061382");
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

        add(card);
    }

    @Override
    public void populateSources(SourceContent container) {
        container.addCode("PaperCard card = new PaperCard();\n"
                + "card.setImage(\"my-image.jpg\");\n"
                + "Element cardContent = ElementFactory.createDiv();\n"
                + "cardContent.setAttribute(\"class\", \"card-content\");\n"
                + "\n"
                + "cardContent.appendChild(ElementFactory.createHeading2(\"Demo card\"));\n"
                + "\n" + "Element cardActions = ElementFactory.createDiv();\n"
                + "cardActions.setAttribute(\"class\",\"card-actions\");\n"
                + "Element buttons = ElementFactory.createDiv();\n"
                + "buttons.setAttribute(\"class\",\"horizontal justified\");\n"
                + "\n" + "cardActions.appendChild(buttons);\n" + "\n"
                + "card.getElement().appendChild(cardContent, cardActions);\n"
                + "\n" + "PaperButton reserve = new PaperButton();\n"
                + "reserve.getElement().setText(\"Reserve\");\n" + "\n"
                + "PaperButton visit = new PaperButton();\n"
                + "visit.getElement().setText(\"Visit page\");\n" + "\n"
                + "buttons.appendChild(visit.getElement()).appendChild(reserve.getElement());\n"
                + "\n" + "layoutContainer.add(card);");
    }
}
