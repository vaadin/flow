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

import com.vaadin.components.paper.spinner.PaperSpinner;
import com.vaadin.components.paper.swatch.picker.PaperSwatchPicker;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.SourceContent;
import com.vaadin.flow.html.Label;

/**
 * View for {@link PaperSpinner} demo.
 */
@ComponentDemo(name = "Paper Swatch Picker", href = "paper-swatch-picker")
public class PaperSwatchPickerView extends DemoView {

    @Override
    public void initView() {
        PaperSwatchPicker picker = new PaperSwatchPicker();
        picker.setColor("#f4511e");
        final Label color = new Label("Picker color: " + picker.getColor());

        // TODO: generator should somehow add the synchronization
        picker.getElement().synchronizeProperty("color", "color-changed");

        picker.addColorChangedListener(
                event -> color.setText("Picker color: " + picker.getColor()));

        add(picker, color);
    }

    @Override
    public void populateSources(SourceContent container) {
        container
                .addCode("PaperSwatchPicker picker = new PaperSwatchPicker();\n"
                        + "picker.setColor(\"#fff\");\n"
                        + "layoutComponent.add(picker);");
    }
}
