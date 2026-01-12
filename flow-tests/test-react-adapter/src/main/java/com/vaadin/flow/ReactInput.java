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
package com.vaadin.flow;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.react.ReactAdapterComponent;
import com.vaadin.flow.function.SerializableConsumer;

@JsModule("./ReactInput.tsx")
@Tag("react-input")
public class ReactInput extends ReactAdapterComponent {

    public ReactInput() {
        this("");
    }

    public ReactInput(String initialValue) {
        setValue(initialValue);
    }

    public String getValue() {
        return getState("value", String.class);
    }

    public void setValue(String value) {
        setState("value", value);
    }

    public void addValueChangeListener(SerializableConsumer<String> onChange) {
        addStateChangeListener("value", String.class, onChange);
    }

}
