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

import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.AllowClientUpdates;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.template.BeanInListingView.ListModel;

@Route(value = "com.vaadin.flow.uitest.ui.template.BeanInListingView", layout = ViewTestLayout.class)
@Tag("listing-bean-view")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/BeanInListing.html")
@JsModule("BeanInListing.js")
public class BeanInListingView extends PolymerTemplate<ListModel> {

    public interface ListModel extends TemplateModel {

        void setUsers(List<User> users);

        @AllowClientUpdates
        List<User> getUsers();

        @AllowClientUpdates
        User getActiveUser();

        void setActiveUser(User user);

        void setSelected(String name);

        void setMessages(List<String> messages);

        @AllowClientUpdates
        String getActiveMessage();
    }

    public static class User {
        private String name;

        @AllowClientUpdates
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    public BeanInListingView() {
        setId("template");
        getModel()
                .setUsers(Arrays.asList(createUser("foo"), createUser("bar")));

        getModel().setMessages(Arrays.asList("baz", "msg"));

        getElement().addPropertyChangeListener("activeUser",
                event -> patientSelected());

        getElement().addPropertyChangeListener("activeMessage",
                event -> messageSelected());

        getModel().setActiveUser(getModel().getUsers().get(0));
    }

    private User createUser(String name) {
        User patient = new User();
        patient.setName(name);
        return patient;
    }

    private void patientSelected() {
        User user = getModel().getActiveUser();
        getModel().setSelected(user.getName());
    }

    private void messageSelected() {
        String msg = getModel().getActiveMessage();
        getModel().setSelected(msg);
    }

}
