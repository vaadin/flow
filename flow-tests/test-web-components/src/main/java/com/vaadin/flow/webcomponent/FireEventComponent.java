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

package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.function.SerializableConsumer;

public class FireEventComponent extends Div {
    private SerializableConsumer<Double> numberConsumer;
    private SerializableConsumer<String> errorConsumer;

    public FireEventComponent() {
        Input number1 = new Input();
        number1.setType("number");
        number1.setId("number1");

        Input number2 = new Input();
        number2.setType("number");
        number2.setId("number2");

        NativeButton button = new NativeButton("Add numbers");
        button.setId("button");

        button.addClickListener(event -> {
            if (numberConsumer != null) {
                try {
                    double n1 = Double.parseDouble(number1.getValue());
                    double n2 = Double.parseDouble(number2.getValue());
                    numberConsumer.accept(n1 + n2);
                } catch (NumberFormatException e) {
                    if (errorConsumer != null) {
                        errorConsumer.accept(e.getMessage());
                    }
                }

            }
        });

        add(number1, number2, button);
    }

    public void setSumConsumer(SerializableConsumer<Double> consumer) {
        this.numberConsumer = consumer;
    }

    public void setErrorConsumer(SerializableConsumer<String> consumer) {
        this.errorConsumer = consumer;
    }
}
