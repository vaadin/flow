/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.nodefeature.ModelMap;
import com.vaadin.flow.template.angular.model.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.ui.AngularTemplate;

@Route(value = "com.vaadin.flow.uitest.ui.PropertyBindingTemplateView", layout = ViewTestLayout.class)
public class PropertyBindingTemplateView extends Div {

    public interface Model extends TemplateModel {
        void setName(String name);

        void setMan(Boolean name);

        void setWeight(double weight);
    }

    public static class PropertyBindingTemplate extends AngularTemplate {

        public PropertyBindingTemplate() {
            super(new ByteArrayInputStream(
                    "<input [value]='name' [booleanprop]='man' [doubleprop]='weight'>"
                            .getBytes(StandardCharsets.UTF_8)));
        }

        @Override
        protected Model getModel() {
            return (Model) super.getModel();
        }
    }

    public PropertyBindingTemplateView() {
        NativeButton setValue = new NativeButton();
        PropertyBindingTemplate input = new PropertyBindingTemplate();

        setProperty(input, "name", "Foo");
        setProperty(input, "man", Boolean.TRUE);
        setProperty(input, "weight", 1.1d);

        setValue.addClickListener(event -> {
            setProperty(input, "name", "Bar");
            setProperty(input, "man", Boolean.FALSE);
            setProperty(input, "weight", 2.2d);
        });

        add(setValue, input);
    }

    private void setProperty(PropertyBindingTemplate input, String property,
            Serializable value) {
        input.getElement().getNode().getFeature(ModelMap.class)
                .setValue(property, value);
    }
}
