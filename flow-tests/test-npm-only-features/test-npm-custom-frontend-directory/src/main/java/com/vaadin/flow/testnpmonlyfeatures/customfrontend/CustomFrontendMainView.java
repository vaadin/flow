/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testnpmonlyfeatures.customfrontend;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.testnpmonlyfeatures.customfrontend.CustomFrontendMainView")
@JavaScript("./javascript.js")
public class CustomFrontendMainView extends Div {

    public CustomFrontendMainView() {
    }
}
