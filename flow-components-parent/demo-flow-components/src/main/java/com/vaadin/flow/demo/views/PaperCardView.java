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
import com.vaadin.flow.component.paper.card.GeneratedPaperCard;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.ComponentDemo.DemoCategory;
import com.vaadin.flow.demo.DemoView;
import com.vaadin.flow.demo.MainLayout;
import com.vaadin.router.Route;

/**
 * View for {@link GeneratedPaperCard} demo.
 */
@Route(value = "paper-card", layout = MainLayout.class)
@ComponentDemo(name = "Paper Card", category = DemoCategory.PAPER)
public class PaperCardView extends DemoView {

    @Override
    public void initView() {
        // begin-source-example
        // source-example-heading: Card with image and actions
        GeneratedPaperCard<?> card = new GeneratedPaperCard<>();
        card.setImage(
                "https://vaadin.com/image/image_gallery?uuid=42c717c0-b63b-4c39-8ee8-3a14b6f477f6&groupId=10187&t=1359960061382");
        Div cardContent = new Div();
        cardContent.addClassName("card-content");

        Div cardActions = new Div();
        cardActions.addClassName("card-actions");

        Div buttons = new Div();
        cardActions.addClassName("horizontal");
        cardActions.addClassName("justified");

        cardActions.add(buttons);

        card.add(cardContent, cardActions);

        GeneratedPaperButton<?> reserve = new GeneratedPaperButton<>();
        reserve.setText("Reserve");

        GeneratedPaperButton<?> visit = new GeneratedPaperButton<>();
        visit.setText("Visit page");

        buttons.add(visit, reserve);
        // end-source-example

        addCard("Card with image and actions", card);
    }
}
