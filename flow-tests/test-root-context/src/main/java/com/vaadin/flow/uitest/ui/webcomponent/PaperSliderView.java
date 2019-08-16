/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.webcomponent;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.webcomponent.PaperSliderView", layout = ViewTestLayout.class)
public class PaperSliderView extends AbstractDivView {
    static final String VALUE_TEXT_ID = "valueText";
    static final String CHANGE_VALUE_ID = "changeValue";
    static final int INITIAL_VALUE = 75;
    static final int UPDATED_VALUE = 50;

    public PaperSliderView() {
        Div valueText = new Div();
        valueText.setId(VALUE_TEXT_ID);
        PaperSlider paperSlider = new PaperSlider();
        paperSlider.setPin(true);
        paperSlider.addValueChangeListener(e -> {
            String text = "Value: " + e.getSource().getValue();
            text += " (set on " + (e.isFromClient() ? "client" : "server")
                    + ')';
            valueText.setText(text);
        });
        paperSlider.setValue(INITIAL_VALUE);
        add(paperSlider, valueText,
                createButton("Set value to " + UPDATED_VALUE, CHANGE_VALUE_ID,
                        e -> paperSlider.setValue(UPDATED_VALUE)));
    }
}
