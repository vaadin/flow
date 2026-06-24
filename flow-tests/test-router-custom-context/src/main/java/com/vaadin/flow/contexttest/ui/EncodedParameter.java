/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.contexttest.ui;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;

@Route("param")
public class EncodedParameter extends Div implements HasUrlParameter<String> {

    public static final String DECODED_CONTENT = "decoded_param_content";
    public static final String ENCODED_CONTENT = "param_content";
    private Div decoded, encoded;

    public EncodedParameter() {
        decoded = new Div();
        decoded.setId(DECODED_CONTENT);
        encoded = new Div();
        encoded.setId(ENCODED_CONTENT);
        add(encoded, decoded);
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        encoded.setText(parameter);
        decoded.setText(URLDecoder.decode(parameter, StandardCharsets.UTF_8));
    }
}
