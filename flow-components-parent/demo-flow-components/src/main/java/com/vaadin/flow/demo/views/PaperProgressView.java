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

import com.vaadin.annotations.StyleSheet;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.ComponentDemo.DemoCategory;
import com.vaadin.flow.html.Label;
import com.vaadin.generated.paper.progress.GeneratedPaperProgress;

/**
 * View for {@link GeneratedPaperProgress} demo.
 */
@ComponentDemo(name = "Paper Progress", href = "paper-progress", category = DemoCategory.PAPER)
@StyleSheet("frontend://src/css/progress.css")
public class PaperProgressView extends DemoView {

    @Override
    public void initView() {
        // begin-source-example
        // source-example-heading: Modified and default indeterminate and static
        // progress:
        // source-example-type: JAVA
        GeneratedPaperProgress slowBlue = new GeneratedPaperProgress();
        slowBlue.setIndeterminate(true);
        slowBlue.getElement().setAttribute("class", "slow blue");

        GeneratedPaperProgress red = new GeneratedPaperProgress();
        red.setIndeterminate(true);
        red.getElement().setAttribute("class", "red");

        GeneratedPaperProgress staticGreen = new GeneratedPaperProgress();
        staticGreen.setValue(45);
        staticGreen.setSecondaryProgress(70);
        // end-source-example

        add(new Label("Indeterminate slow blue"), slowBlue,
                new Label("Indeterminate red"), red,
                new Label("Static green with 2 states"), staticGreen);
    }

    // @formatter:off
    /*
    // begin-source-example
    // source-example-heading: Style sheet:
    // source-example-type: CSS
    paper-progress.blue {
        --paper-progress-active-color: var(--paper-light-blue-500);
        --paper-progress-secondary-color: var(--paper-light-blue-100);
    }

    paper-progress.slow {
        --paper-progress-indeterminate-cycle-duration: 5s;
    }
    // end-source-example
    */
    // @formatter:on
}
