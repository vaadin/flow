/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.UIElementView")
public class UIElementView extends AbstractDivView {

    public UIElementView() {
        getElement().executeJs(getJs());

        NativeButton attachElement = new NativeButton("Attach Element via JS",
                event -> attachElement());
        add(attachElement);
    }

    private void attachElement() {
        getElement().executeJs(getJs());
    }

    private String getJs() {
        return "var newElement = document.createElement('div');"
                + "newElement.className='body-child';"
                + "this.appendChild(newElement);";
    }
}
