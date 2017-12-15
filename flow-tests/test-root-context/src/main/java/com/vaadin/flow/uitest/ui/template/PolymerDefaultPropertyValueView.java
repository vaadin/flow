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
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.model.TemplateModel;
import com.vaadin.flow.uitest.ui.AbstractDivView;
import com.vaadin.flow.router.Route;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.common.PropertyDescriptor;
import com.vaadin.ui.common.PropertyDescriptors;
import com.vaadin.ui.event.Synchronize;
import com.vaadin.ui.polymertemplate.PolymerTemplate;

@Route("com.vaadin.flow.uitest.ui.template.PolymerDefaultPropertyValueView")
public class PolymerDefaultPropertyValueView extends AbstractDivView {

    private static final PropertyDescriptor<String, String> msgDescriptor = PropertyDescriptors
            .propertyWithDefault("message", "");

    public interface MyModel extends TemplateModel {
        void setText(String text);

        void setName(String name);
    }

    @Tag("default-property")
    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/PolymerDefaultPropertyValue.html")
    public static class MyTemplate extends PolymerTemplate<MyModel> {

        public MyTemplate() {
            getModel().setText("foo");
            setMessage("updated-message");
        }

        @Synchronize("email-changed")
        public String getEmail() {
            return getElement().getProperty("email");
        }

        @Synchronize("message-changed")
        public String getMessage() {
            return get(msgDescriptor);
        }

        public void setMessage(String value) {
            set(msgDescriptor, value);
        }

    }

    public PolymerDefaultPropertyValueView() {
        MyTemplate template = new MyTemplate();
        template.setId("template");
        add(template);

        NativeButton button = new NativeButton("Show email value",
                event -> createEmailValue(template));
        button.setId("show-email");
        add(button);
    }

    private void createEmailValue(MyTemplate template) {
        Div div = new Div();
        div.setText(template.getEmail());
        div.setId("email-value");
        add(div);
    }

}
