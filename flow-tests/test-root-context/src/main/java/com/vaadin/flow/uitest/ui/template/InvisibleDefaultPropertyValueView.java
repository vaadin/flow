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
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route("com.vaadin.flow.uitest.ui.template.InvisibleDefaultPropertyValueView")
public class InvisibleDefaultPropertyValueView extends AbstractDivView {

    public InvisibleDefaultPropertyValueView() {
        PolymerDefaultPropertyValue template = new PolymerDefaultPropertyValue();
        template.setVisible(false);
        template.setId("template");
        add(template);

        Div div = new Div();
        div.setId("email-value");
        add(div);

        add(createButton("Show email value", "show-email",
                event -> div.setText(template.getEmail())));

        add(createButton("Make template visible", "set-visible",
                event -> template.setVisible(true)));
    }

}
