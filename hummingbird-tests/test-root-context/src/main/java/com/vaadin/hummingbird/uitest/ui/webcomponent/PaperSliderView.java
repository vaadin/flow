/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.ui.webcomponent;

import com.vaadin.hummingbird.html.Button;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.router.View;

public class PaperSliderView extends Div implements View {
    static final String VALUE_TEXT_ID = "valueText";
    static final String CHANGE_VALUE_ID = "changeValue";

    public PaperSliderView() {
        Div valueText = new Div();
        valueText.setId(VALUE_TEXT_ID);
        PaperSlider paperSlider = new PaperSlider();
        paperSlider.setPin(true);
        paperSlider.addValueChangeListener(e -> {
            String text = "Value: " + e.getSource().getValue();
            text += " (set on " + (e.isFromClient() ? "client" : "server")
                    + ")";
            valueText.setText(text);
        });
        paperSlider.setValue(75);
        Button changeValueFromServer = new Button("Set value to 50",
                e -> paperSlider.setValue(50));
        changeValueFromServer.setId(CHANGE_VALUE_ID);
        add(paperSlider, valueText, changeValueFromServer);
    }
}
