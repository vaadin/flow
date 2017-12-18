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

import com.vaadin.flow.component.paper.input.GeneratedPaperInput;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.ComponentDemo.DemoCategory;
import com.vaadin.flow.demo.DemoView;
import com.vaadin.flow.demo.MainLayout;
import com.vaadin.router.Route;

/**
 * View for {@link GeneratedPaperInput} demo.
 */
@Route(value = "paper-input", layout = MainLayout.class)
@ComponentDemo(name = "Paper Input", category = DemoCategory.PAPER)
public class PaperInputView extends DemoView {

    @Override
    public void initView() {
        // begin-source-example
        // source-example-heading: Basic input
        GeneratedPaperInput<?> input = new GeneratedPaperInput<>();
        input.setLabel("Name");
        input.setPlaceholder("John Doe");
        // end-source-example

        addCard("Basic input", input);
    }
}
