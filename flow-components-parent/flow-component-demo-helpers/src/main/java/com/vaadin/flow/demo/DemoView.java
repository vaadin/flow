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
package com.vaadin.flow.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.common.HasComponents;
import com.vaadin.ui.common.JavaScript;
import com.vaadin.ui.common.StyleSheet;
import com.vaadin.ui.event.AttachEvent;
import com.vaadin.ui.html.Div;
import com.vaadin.ui.html.H3;

/**
 * Base class for all the Views that demo some component.
 * 
 * @author Vaadin Ltd
 */
@StyleSheet("src/css/demo.css")
@JavaScript("src/script/prism.js")
public abstract class DemoView extends Div implements HasComponents {

    private Map<String, List<SourceCodeExample>> sourceCodeExamples = new HashMap<>();

    protected DemoView() {
        try {
            getElement().setAttribute("class", "demo-view");

            populateSources();
            initView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI.getCurrent().getPage().executeJavaScript("Prism.highlightAll();");
    }

    /**
     * Method run where the actual view builds its contents.
     */
    protected abstract void initView();

    /**
     * When called the view should populate the given SourceContainer with
     * sample source code to be shown.
     */
    public void populateSources() {
        SourceContentResolver.getSourceCodeExamplesForClass(getClass())
                .forEach(this::putSourceCode);
    }

    private void putSourceCode(SourceCodeExample example) {
        String heading = example.getHeading();
        List<SourceCodeExample> list = sourceCodeExamples
                .computeIfAbsent(heading, key -> new ArrayList<>());
        list.add(example);
    }

    /**
     * Create and add a new component card to the view. It automatically adds
     * any source code examples with the same heading to the bottom of the card.
     *
     * @param heading
     *            the header text of the card, that is added to the layout. If
     *            <code>null</code> or empty, the header is not added
     *
     * @param components
     *            components to add on creation. If <code>null</code> or empty,
     *            the card is created without the components inside
     * @return created component container card.
     */
    public Card addCard(String heading, Component... components) {
        if (heading != null && !heading.isEmpty()) {
            add(new H3(heading));
        }

        Card card = new Card();
        if (components != null && components.length > 0) {
            card.add(components);
        }

        List<SourceCodeExample> list = sourceCodeExamples.get(heading);
        if (list != null) {
            list.stream().map(this::createSourceContent).forEach(card::add);
        }

        add(card);

        return card;
    }

    private SourceContent createSourceContent(
            SourceCodeExample sourceCodeExample) {
        SourceContent content = new SourceContent();
        String sourceString = sourceCodeExample.getSourceCode();
        switch (sourceCodeExample.getSourceType()) {
        case CSS:
            content.addCss(sourceString);
            break;
        case JAVA:
            content.addCode(sourceString);
            break;
        case UNDEFINED:
        default:
            content.addCode(sourceString);
            break;
        }
        return content;
    }
}
