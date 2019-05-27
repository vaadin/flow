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

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.TwoWayPolymerBindingView", layout = ViewTestLayout.class)
@Tag("my-template")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/TwoWayPolymerBinding.html")
@JsModule("TwoWayPolymerBinding.js")
public class TwoWayPolymerBindingView
        extends PolymerTemplate<TwoWayPolymerBindingView.TwoWayModel> {
    public TwoWayPolymerBindingView() {
        setId("template");

        getElement().addPropertyChangeListener("value",
                event -> valueUpdated());
    }

    public interface TwoWayModel extends TemplateModel {
        void setValue(String value);

        String getValue();

        void setStatus(String status);
    }

    @EventHandler
    private void valueUpdated() {
        getModel().setStatus("Value: " + getModel().getValue());
    }

    @EventHandler
    private void resetValue() {
        getModel().setValue("");
        valueUpdated();
    }
}
