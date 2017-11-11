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
import com.vaadin.flow.demo.ComponentDemo.DemoCategory;
import com.vaadin.flow.demo.DemoView;
import com.vaadin.flow.demo.MainLayout;
import com.vaadin.router.Route;
import com.vaadin.ui.html.Label;
import com.vaadin.ui.paper.spinner.GeneratedPaperSpinner;
import com.vaadin.ui.paper.swatchpicker.GeneratedPaperSwatchPicker;

/**
 * View for {@link GeneratedPaperSpinner} demo.
 */
@Route(value = "paper-swatch-picker", layout = MainLayout.class)
@ComponentDemo(name = "Paper Swatch Picker", category = DemoCategory.PAPER)
public class PaperSwatchPickerView extends DemoView {

    @Override
    public void initView() {
        // begin-source-example
        // source-example-heading: Basic color selector
        GeneratedPaperSwatchPicker picker = new GeneratedPaperSwatchPicker();
        picker.setColor("#f4511e");
        final Label color = new Label("Picker color: " + picker.getColor());

        picker.addColorChangeListener(
                event -> color.setText("Picker color: " + picker.getColor()));
        // end-source-example

        addCard("Basic color selector", picker, color);
    }
}
