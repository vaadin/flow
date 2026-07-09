/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.router.Route;
import java.time.LocalDateTime;

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
