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
@StyleSheet("frontend://src/progress.css")
public class PaperProgressView extends DemoView {

    @Override
    public void initView() {
        PaperProgress slowBlue = new PaperProgress();

        slowBlue.getElement().setAttribute("indeterminate", "");
        slowBlue.getElement().setAttribute("class", "slow blue");

        PaperProgress red = new PaperProgress();

        red.getElement().setAttribute("indeterminate", "");
        red.getElement().setAttribute("class", "red");

        PaperProgress staticGreen = new PaperProgress();

        staticGreen.getElement().setAttribute("value", "45");
        staticGreen.getElement().setAttribute("secondary-progress", "70");

        add(new Label("Indeterminate slow blue"), slowBlue, new Label("Indeterminate red"), red, new Label("Static green with 2 states"),staticGreen);
    }
}
