/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.ui;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.hummingbird.html.Button;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.router.View;
import com.vaadin.ui.Template;

/**
 * @author Vaadin Ltd
 *
 */
public class PropertyBindingTemplateView extends Div implements View {

    public static class PropertyBindingTemplate extends Template {

        public PropertyBindingTemplate() {
            super(new ByteArrayInputStream(
                    "<input [value]='name'>".getBytes(StandardCharsets.UTF_8)));
        }
    }

    public PropertyBindingTemplateView() {
        Button button = new Button();
        PropertyBindingTemplate input = new PropertyBindingTemplate();
        setProperty(input, "Foo");
        button.addClickListener(event -> setProperty(input, "Bar"));
        add(button, input);
    }

    private void setProperty(PropertyBindingTemplate input, String name) {
        input.getElement().getNode().getFeature(ModelMap.class).setValue("name",
                name);
    }
}
