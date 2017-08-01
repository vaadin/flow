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
import com.vaadin.generated.paper.spinner.GeneratedPaperSpinner;

/**
 * View for {@link GeneratedPaperSpinner} demo.
 */
@ComponentDemo(name = "Paper Spinner", href = "paper-spinner")
public class PaperSpinnerView extends DemoView {

    @Override
    public void initView() {
        GeneratedPaperSpinner paperSpinner = new GeneratedPaperSpinner();
        paperSpinner.setActive(true);
        add(paperSpinner);
    }

    @Override
    public void populateSources(SourceContent container) {
        container.addCode("PaperSpinner paperSpinner = new PaperSpinner();\n"
                + "paperSpinner.setActive(true);\n"
                + "parentComponent.add(paperSpinner);");
    }
}
