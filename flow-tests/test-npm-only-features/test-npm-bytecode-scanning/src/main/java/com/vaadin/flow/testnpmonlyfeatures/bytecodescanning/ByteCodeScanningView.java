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
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;

@Route("com.vaadin.flow.testnpmonlyfeatures.bytecodescanning.ByteCodeScanningView")
public class ByteCodeScanningView extends Div {

    public static final String MODE_LABEL_ID = "modeLabel";
    public static final String COMPONENT_ID = "myButton";
    public static final String COMPONENT_MISSING_LABEL_ID = "componentMissingLabel";

    public ByteCodeScanningView() throws Exception {
        /*
         * Add a label so that the IT can read the current mode.
         */
        boolean production = VaadinService.getCurrent()
                .getDeploymentConfiguration().isProductionMode();
        Label modeLabel = new Label(Boolean.toString(production));
        modeLabel.setId(MODE_LABEL_ID);
        add(modeLabel);

        try {
            /*
             * Create component using reflection, so that use will not be
             * detected by the bytecode scanner.
             */
            Class<?> clazz = Class.forName("com.vaadin.flow.testnpmonlyfeatures.bytecodescanning.MyButton");
            Component button = (Component) clazz.newInstance();
            button.setId(COMPONENT_ID);
            add(button);
        } catch (IllegalStateException e) {
            /*
             * IllegalStateException will be thrown by NpmTemplateParser::getTemplateContent
             * if bytecode scanning is enabled (since MyButton is not reachable).
             * Add a simple label that can be verified in the IT. If bytecode
             * scanning is disabled, we should never get here.
             */
            Label componentMissingLabel = new Label(e.getMessage());
            componentMissingLabel.setId(COMPONENT_MISSING_LABEL_ID);
            add(componentMissingLabel);
        }
    }
}
