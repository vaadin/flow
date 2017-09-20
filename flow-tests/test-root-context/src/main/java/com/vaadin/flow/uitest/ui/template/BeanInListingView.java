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

import java.util.Arrays;
import java.util.List;

import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.event.Tag;
import com.vaadin.flow.router.View;
import com.vaadin.ui.polymertemplate.PolymerTemplate;
import com.vaadin.flow.model.TemplateModel;
import com.vaadin.flow.uitest.ui.template.BeanInListingView.ListModel;

@Tag("listing-bean-view")
@HtmlImport("/com/vaadin/flow/uitest/ui/template/BeanInListing.html")
public class BeanInListingView extends PolymerTemplate<ListModel>
        implements View {

    public interface ListModel extends TemplateModel {

        void setUsers(List<User> users);

        List<User> getUsers();

        User getActiveUser();

        void setActiveUser(User user);

        void setSelected(String name);

        void setMessages(List<String> messages);

        String getActiveMessage();
    }

    public static class User {
        private String name;

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
