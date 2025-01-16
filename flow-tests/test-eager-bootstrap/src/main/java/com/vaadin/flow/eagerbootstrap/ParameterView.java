/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.eagerbootstrap;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("parameter")
public class ParameterView extends Div implements HasUrlParameter<String> {

    private Div parameters;

    public ParameterView() {
        setId("view");

        RouterLink fooLink = new RouterLink("Navigate with parameter 'foo'",
                ParameterView.class, "foo");
        fooLink.setId("fooLink");
        add(fooLink);
        add(new Div());
        RouterLink barLink = new RouterLink("Navigate with parameter 'bar'",
                ParameterView.class, "bar");
        barLink.setId("barLink");
        add(barLink);

        parameters = new Div();
        parameters.setId("parameters");
        parameters.getStyle()
                .setWhiteSpace(com.vaadin.flow.dom.Style.WhiteSpace.PRE);
        add(parameters);

    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        parameters.setText(parameters.getText() + "\n"
                + "setParameter called with: " + parameter);
    }
}
