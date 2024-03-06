/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.PreserveOnRefreshReAddView")
@PreserveOnRefresh
public class PreserveOnRefreshReAddView extends Div {

    public PreserveOnRefreshReAddView() {
        Text text1 = new Text("Text");
        Text text2 = new Text("Another Text");

        Div container = new Div();
        container.setId("container");

        NativeButton setText = new NativeButton("Set text", e -> {
            container.removeAll();
            container.add(text1);
        });
        NativeButton setAnotherText = new NativeButton("Set another text",
                e -> {
                    container.removeAll();
                    container.add(text2);
                });

        setText.setId("set-text");
        setAnotherText.setId("set-another-text");

        add(setText, setAnotherText, container);
    }
}
