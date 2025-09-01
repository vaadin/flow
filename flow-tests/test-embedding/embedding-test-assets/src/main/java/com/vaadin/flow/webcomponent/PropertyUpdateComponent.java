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

package com.vaadin.flow.webcomponent;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.node.BaseJsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.internal.JacksonUtils;

@Tag("click-counter")
public class PropertyUpdateComponent extends Div {
    @FunctionalInterface
    public interface NumberListener {
        void handleNumber(int number);
    }

    private int clickCounter = 0;
    private Set<NumberListener> listenerSet = new HashSet<>();

    public PropertyUpdateComponent() {
        NativeButton nativeButton = new NativeButton("Click me!");
        nativeButton.addClickListener(event -> {
            clickCounter++;
            publishNumber();
        });

        add(nativeButton);
    }

    private void publishNumber() {
        listenerSet.forEach(
                numberListener -> numberListener.handleNumber(clickCounter));
    }

    public BaseJsonNode getNumberJson() {
        ObjectNode json = JacksonUtils.createObjectNode();
        json.put("counter", clickCounter);
        return json;
    }

    public void addListener(NumberListener listener) {
        listenerSet.add(listener);
    }
}
