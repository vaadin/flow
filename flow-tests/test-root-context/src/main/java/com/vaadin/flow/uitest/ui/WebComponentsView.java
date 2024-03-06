/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.Route;

/**
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
@Route("com.vaadin.flow.uitest.ui.WebComponentsView")
public class WebComponentsView extends AbstractDivView {

    public WebComponentsView() {
        Element div = ElementFactory.createDiv("Web components v1");
        getElement().appendChild(div);
    }
}
