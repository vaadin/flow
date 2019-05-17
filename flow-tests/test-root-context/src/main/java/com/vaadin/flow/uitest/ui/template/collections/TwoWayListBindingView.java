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
package com.vaadin.flow.uitest.ui.template.collections;

import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.AllowClientUpdates;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.template.collections.TwoWayListBindingView", layout = ViewTestLayout.class)
@Tag("two-way-list-binding")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/collections/TwoWayListBinding.html")
@JsModule("TwoWayListBinding.js")
public class TwoWayListBindingView
        extends PolymerTemplate<TwoWayListBindingView.TwoWayBindingModel>
        implements HasComponents {

    public interface TwoWayBindingModel extends TemplateModel {

        void setMessages(List<Message> messages);

        @AllowClientUpdates
        List<Message> getMessages();

        void setEnable(boolean enable);
    }

    public TwoWayListBindingView() {
        getModel().setMessages(
                Arrays.asList(new Message("foo"), new Message("bar")));

        add(AbstractDivView.createButton("Show listing", "enable",
                event -> getModel().setEnable(true)));
    }

    @EventHandler
    private void valueUpdated() {
        Div div = new Div();
        div.setClassName("messages");
        div.setText(getModel().getMessages().toString());
        add(div);
    }
}
