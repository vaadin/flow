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
package com.vaadin.flow.uitest.ui;

import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.SerializeUIView", layout = ViewTestLayout.class)
public class SerializeUIView extends AbstractDivView {
    public SerializeUIView() {
        Div label = new Div();
        label.setId("message");

        NativeButton button = createButton("Serialize", "serialize", event -> {
            UI ui = UI.getCurrent();
            try {
                byte[] serialize = SerializationUtils.serialize(ui);

                String result = serialize.length > 0 ?
                        "Successfully serialized ui" :
                        "Serialization failed";
                label.setText(result);
            }catch(SerializationException se) {
                label.setText(se.getMessage());
            }
        });

        add(label, button);
    }
}
