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

import com.vaadin.flow.model.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.Route;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.polymertemplate.EventHandler;
import com.vaadin.ui.polymertemplate.PolymerTemplate;

@Route(value = "com.vaadin.flow.uitest.ui.template.TwoWayPolymerBindingView", layout = ViewTestLayout.class)
@Tag("my-template")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/TwoWayPolymerBinding.html")
public class TwoWayPolymerBindingView
        extends PolymerTemplate<TwoWayPolymerBindingView.TwoWayModel> {
    public TwoWayPolymerBindingView() {
        setId("template");
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
