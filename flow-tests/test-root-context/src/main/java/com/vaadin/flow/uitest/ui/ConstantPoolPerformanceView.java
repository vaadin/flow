/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

/* Manual test case for #7826 */
@Route("performance/constant-pool")
public class ConstantPoolPerformanceView extends AbstractDivView {

    Div container = new Div();
    Div notification = new Div();

    public ConstantPoolPerformanceView() {
        NativeButton btn = new NativeButton(
                "add 2k divs without click listener");
        btn.addClickListener(e -> {
            container.removeAll();
            for (int i = 0; i < 2000; i++) {
                container.add(new NativeButton("No click listener "));
            }
        });
        NativeButton btn2 = new NativeButton("add 2k divs with click listener");
        btn2.addClickListener(e -> {
            container.removeAll();
            for (int i = 0; i < 2000; i++) {
                container.add(new NativeButton("With click listener " + i,
                        e2 -> notification.setText("clicked")));
            }
        });
        final NativeButton clearButtons = new NativeButton("clear buttons",
                e -> container.removeAll());
        add(btn, btn2, clearButtons, notification, container);
    }

}
