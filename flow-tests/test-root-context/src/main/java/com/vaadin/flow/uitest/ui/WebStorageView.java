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
package com.vaadin.flow.uitest.ui;

import java.time.LocalDateTime;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.WebStorageView")
public class WebStorageView extends Div {

    static final String VALUE_NOT_SET = "Value is not currently set for the key";

    public WebStorageView() {
        Input value = new Input();
        value.setId("input");
        NativeButton setData = new NativeButton();
        NativeButton detect = new NativeButton();
        NativeButton detectCF = new NativeButton();
        NativeButton remove = new NativeButton();
        NativeButton clear = new NativeButton();
        Div msg = new Div();
        msg.setId("msg");
        add(value, setData, detect, detectCF, remove, clear, msg);

        value.setValue(LocalDateTime.now().toString());

        setData.setText("setText");
        setData.setId("setText");
        setData.addClickListener(e -> {
            WebStorage.setItem("test", value.getValue());
        });

        detect.setText("Detect");
        detect.setId("detect");
        detect.addClickListener(e -> {
            WebStorage.getItem("test", v -> {
                if (v == null) {
                    msg.setText(VALUE_NOT_SET);
                } else {
                    msg.setText(v);
                }
            });
        });

        detectCF.setText("Detect CompletableFuture");
        detectCF.setId("detectCF");
        detectCF.addClickListener(e -> {
            WebStorage.getItem("test").thenAccept(v -> {
                if (v == null) {
                    msg.setText(VALUE_NOT_SET);
                } else {
                    msg.setText(v);
                }
            });
        });

        remove.setText("Remove 'test'");
        remove.setId("remove");
        remove.addClickListener(e -> {
            WebStorage.removeItem("test");
        });

        clear.setText("Clear all");
        clear.setId("clear");
        clear.addClickListener(e -> {
            WebStorage.clear();
        });

    }

}
