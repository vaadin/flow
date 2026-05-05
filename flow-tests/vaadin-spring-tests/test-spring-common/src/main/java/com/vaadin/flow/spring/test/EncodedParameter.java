/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.spring.test;

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
