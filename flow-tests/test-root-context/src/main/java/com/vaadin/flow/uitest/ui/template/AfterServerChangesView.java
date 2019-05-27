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

import java.util.UUID;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.template.AfterServerChangesView", layout = ViewTestLayout.class)
public class AfterServerChangesView extends AbstractDivView {

    @Tag("after-server-changes")
    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/AfterServerChanges.html")
    @JsModule("AfterServerChanges.js")
    public static class AfterServerChanges extends PolymerTemplate<Message> {

        @Override
        protected Message getModel() {
            return super.getModel();
        }
    }

    public AfterServerChangesView() {
        add(new AfterServerChanges());
        AfterServerChanges component = new AfterServerChanges();
        add(component);

        add(new OneWayPolymerBindingView());

        add(createButton("Remove the second component", "remove",
                event -> remove(component)));

        add(createButton("Update components", "update",
                event -> updateComponents()));
    }

    private void updateComponents() {
        getChildren()
                .filter(component -> component.getClass()
                        .equals(AfterServerChanges.class))
                .map(AfterServerChanges.class::cast)
                .forEach(component -> component.getModel()
                        .setText(UUID.randomUUID().toString()));
    }

}
