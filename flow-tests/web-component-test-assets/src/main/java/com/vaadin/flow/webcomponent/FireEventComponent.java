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
    public enum OptionsType {NoBubble_NoCancel, Bubble_NoCancel, Bubble_Cancel}

    private SerializableConsumer<Double> numberConsumer;
    private SerializableConsumer<String> errorConsumer;
    private SerializableConsumer<OptionsType> buttonConsumer;

    public FireEventComponent() {
        Input number1 = new Input();
        number1.setId("number1");

        Input number2 = new Input();
        number2.setId("number2");

        NativeButton button = new NativeButton("Add numbers");
        button.setId("button");

        button.addClickListener(event -> {
            try {
                double n1 = Double.parseDouble(number1.getValue());
                double n2 = Double.parseDouble(number2.getValue());
                dispatchSum(n1, n2);
            } catch (NumberFormatException e) {
                dispatchError(e.getMessage());
            }
        });

        add(number1, number2, button);

        Div div = new Div();
        NativeButton button1 = new NativeButton("Button 1",
                event -> buttonConsumer.accept(OptionsType.NoBubble_NoCancel));
        button1.setId("b1");
        NativeButton button2 = new NativeButton("Button 2",
                event -> buttonConsumer.accept(OptionsType.Bubble_NoCancel));
        button2.setId("b2");
        NativeButton button3 = new NativeButton("Button 3",
                event -> buttonConsumer.accept(OptionsType.Bubble_Cancel));
        button3.setId("b3");
        div.add(button1, button2, button3);
        add(div);
    }

    public void setSumConsumer(SerializableConsumer<Double> consumer) {
        this.numberConsumer = consumer;
    }

    public void setErrorConsumer(SerializableConsumer<String> consumer) {
        this.errorConsumer = consumer;
    }

    public void setButtonConsumer(SerializableConsumer<OptionsType> consumer) {
        this.buttonConsumer = consumer;
    }

    private void dispatchSum(double n1, double n2) {
        if (numberConsumer != null) {
            numberConsumer.accept(n1 + n2);
        }
    }

    private void dispatchError(String errorMessage) {
        if (errorConsumer != null) {
            errorConsumer.accept(errorMessage);
        }
    }
}
