/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.littemplate;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.littemplate.ReattachView")
public class ReattachView extends Div {

    public ReattachView() {
        TestForm testForm = new TestForm();
        testForm.setId("form-template");

        NativeButton button = new NativeButton("Click");
        button.addClickListener(ce -> {
            if (testForm.isAttached()) {
                remove(testForm);
            } else {
                add(testForm);
            }
        });
        button.setId("click");
        add(button);
    }
}
