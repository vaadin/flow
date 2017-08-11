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

import java.util.HashMap;
import java.util.Map;

import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.demo.SourceContent;
import com.vaadin.flow.demo.SourceContentResolver;
import com.vaadin.flow.demo.model.SourceCodeExample;
import com.vaadin.flow.html.H3;
import com.vaadin.flow.router.View;
import com.vaadin.ui.AttachEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.UI;

/**
 * Base class for all the Views that demo some component.
 */
@Tag("div")
@StyleSheet("frontend://src/css/demo.css")
public abstract class DemoView extends Component
        implements View, HasComponents {

    private Map<String, SourceCodeExample> sourceCodeExamples = new HashMap<>();

    protected DemoView() {
        getElement().setAttribute("class", "demo-view");

        populateSources();
        initView();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI.getCurrent().getPage().executeJavaScript("Prism.highlightAll();");
    }

    /**
     * Method run where the actual view builds its contents.
     */
    abstract void initView();

    /**
     * When called the view should populate the given SourceContainer with
     * sample source code to be shown.
     * 
     * @param container
     *            sample source code container.
     */
    public void populateSources() {
        SourceContentResolver.getSourceCodeExamplesForClass(getClass())
                .forEach(example -> sourceCodeExamples.put(example.getHeading(),
                        example));
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

        SourceCodeExample sourceCodeExample = sourceCodeExamples.get(heading);
        if (sourceCodeExample != null) {
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
            card.add(content);
        }

        add(card);

        return card;
    }
}
