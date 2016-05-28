/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.annotations.Id;
import com.vaadin.hummingbird.html.Button;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.nodefeature.TemplateMap;
import com.vaadin.hummingbird.router.View;
import com.vaadin.ui.Template;

public class BasicTemplateView extends Template implements View {

    @Id("container")
    private Div container;

    public BasicTemplateView() {
        assert container != null;

        Button button = new Button(
                "Element added to template (click to remove)");
        button.addClickListener(e -> container.remove(button));
        container.add(button);

        Button childSlotContent = new Button(
                "Child slot content (click to remove)");
        childSlotContent.addClassName("childSlotContent");

        // Will introduce a nicer API in a separate patch
        childSlotContent.addClickListener(e -> {
            getElement().getNode().getFeature(TemplateMap.class).setChild(null);
        });
        getElement().getNode().getFeature(TemplateMap.class)
                .setChild(childSlotContent.getElement().getNode());
    }
}
