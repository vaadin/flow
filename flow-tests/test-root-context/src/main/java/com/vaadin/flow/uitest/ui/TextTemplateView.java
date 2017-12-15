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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.template.angular.model.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.TextTemplateView", layout = ViewTestLayout.class)
public class TextTemplateView extends Div {

    public interface Model extends TemplateModel {
        void setName(String name);
    }

    public TextTemplateView() {
        NativeButton button = new NativeButton("Update simple name");
        button.setId("set-simple-name");
        InlineTemplate<Model> text = new InlineTemplate<>(
                "<div id='text'>{{name}}</div>", Model.class);
        setName(text, "Foo", "plain-text");
        button.addClickListener(event -> setName(text, "Bar", "plain-text"));
        add(button, text);

        InlineTemplate<Model> jsExpression = new InlineTemplate<>(
                "<div id='js-expression'>{{name ? 'Hello ' + name: 'No name'}}</div>",
                Model.class);
        setName(jsExpression, null, "js-text");
        NativeButton updateJsExpression = new NativeButton(
                "Update JS expression");
        updateJsExpression.setId("set-expression-name");
        updateJsExpression.addClickListener(
                event -> setName(jsExpression, "Foo", "js-text"));
        add(updateJsExpression, jsExpression);
    }

    private void setName(InlineTemplate<Model> template, String name,
            String serverSideClass) {
        template.getModel().setName(name);
        String property = template.getElement().getTextRecursively();
        Label label = new Label(property);
        label.addClassName(serverSideClass);
        add(label);
    }

}
