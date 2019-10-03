/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.testnpmonlyfeatures.bytecodescanning;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.testnpmonlyfeatures.bytecodescanning.ByteCodeScanningView")
public class ByteCodeScanningView extends Div {

    public static final String MODE_LABEL_ID = "modeLabel";
    public static final String COMPONENT_ID = "myButton";

    public ByteCodeScanningView() throws Exception {
        Class<?> clazz = Class.forName(
                "com.vaadin.flow.testnpmonlyfeatures.bytecodescanning.MyButton");
        Component button = (Component) clazz.newInstance();
        button.setId(COMPONENT_ID);
        add(button);
    }
}
