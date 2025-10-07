/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.component.html.testbench;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.RangeInput;
import com.vaadin.flow.router.Route;

@Route("RangeInput")
public class RangeInputView extends Div {

    public RangeInputView() {
        Div log = new Div();
        log.setId("log");

        RangeInput input = new RangeInput();
        input.setId("input");
        input.addValueChangeListener(e -> {
            log.setText("Value is '" + input.getValue() + "'");
        });
        add(log, input);
    }
}
