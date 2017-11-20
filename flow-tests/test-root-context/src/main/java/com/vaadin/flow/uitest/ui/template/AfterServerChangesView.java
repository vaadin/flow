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

import java.util.UUID;

import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;
import com.vaadin.router.Route;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.html.NativeButton;
import com.vaadin.ui.polymertemplate.PolymerTemplate;

@Route(value = "com.vaadin.flow.uitest.ui.template.AfterServerChangesView", layout = ViewTestLayout.class)
public class AfterServerChangesView extends AbstractDivView {

    @Tag("after-server-changes")
    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/AfterServerChanges.html")
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

        NativeButton button = new NativeButton("Remove the second component",
                event -> remove(component));
        button.setId("remove");
        add(button);

        NativeButton update = new NativeButton("Update components",
                event -> updateComponents());
        update.setId("update");
        add(update);
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
