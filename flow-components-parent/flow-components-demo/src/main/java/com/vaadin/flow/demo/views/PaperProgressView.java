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
import com.vaadin.components.paper.progress.PaperProgress;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.html.Label;

/**
 * View for {@link PaperProgress} demo.
 */
@ComponentDemo(name = "Paper Progress", href = "paper-progress")
@StyleSheet("frontend://src/css/progress.css")
public class PaperProgressView extends DemoView {

    @Override
    public void initView() {
        // begin-source-example
        // source-example-heading: Modified and default indeterminate and static progress:
        // source-example-type: JAVA
        PaperProgress slowBlue = new PaperProgress();
        slowBlue.setIndeterminate(true);
        slowBlue.getElement().setAttribute("class", "slow blue");

        PaperProgress red = new PaperProgress();
        red.setIndeterminate(true);
        red.getElement().setAttribute("class", "red");

        PaperProgress staticGreen = new PaperProgress();
        staticGreen.setValue(45);
        staticGreen.setSecondaryProgress(70);
        // end-source-example

        add(new Label("Indeterminate slow blue"), slowBlue,
                new Label("Indeterminate red"), red,
                new Label("Static green with 2 states"), staticGreen);
    }

    // @formatter::off
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
    // @formatter::on
}
