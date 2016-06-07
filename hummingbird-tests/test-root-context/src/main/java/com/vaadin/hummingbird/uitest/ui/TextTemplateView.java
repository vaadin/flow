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

import com.vaadin.hummingbird.html.Button;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.router.View;
import com.vaadin.hummingbird.template.model.TemplateModel;

/**
 * @author Vaadin Ltd
 *
 */
public class TextTemplateView extends Div implements View {

    public TextTemplateView() {
        Button button = new Button();
        InlineTemplate<TemplateModel> text = new InlineTemplate<>(
                "<div id='text'>{{name}}</div>", TemplateModel.class);
        setName(text, "Foo");
        button.addClickListener(event -> setName(text, "Bar"));
        add(button, text);
    }

    private void setName(InlineTemplate<?> template, String name) {
        template.getElement().getNode().getFeature(ModelMap.class)
                .setValue("name", name);
    }
}
