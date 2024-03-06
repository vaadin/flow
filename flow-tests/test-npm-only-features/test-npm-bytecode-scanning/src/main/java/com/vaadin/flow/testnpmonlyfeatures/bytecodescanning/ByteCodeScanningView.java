/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testnpmonlyfeatures.bytecodescanning;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.testnpmonlyfeatures.bytecodescanning.ByteCodeScanningView")
public class ByteCodeScanningView extends Div {

    public static final String COMPONENT_ID = "myButton";

    public ByteCodeScanningView() throws Exception {
        Class<?> clazz = Class.forName(
                "com.vaadin.flow.testnpmonlyfeatures.bytecodescanning.MyButton");
        Component button = (Component) clazz.newInstance();
        button.setId(COMPONENT_ID);
        add(button);
    }
}
