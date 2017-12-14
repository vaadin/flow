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

import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.Route;
import com.vaadin.ui.common.ClientDelegate;
import com.vaadin.ui.polymertemplate.Id;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.template.angular.model.TemplateModel;
import com.vaadin.ui.AngularTemplate;

@Route(value = "com.vaadin.flow.uitest.ui.template.JsInTemplateView", layout = ViewTestLayout.class)
public class JsInTemplateView extends AngularTemplate {
    public interface JsInTemplateModel extends TemplateModel {
        String getFirstName();

        void setFirstName(String firstName);

        int getItemCount();

        void setItemCount(int itemCount);

        void setPerson(Person person);

        void setPersonHolder(PersonHolder holder);
    }

    public static class PersonHolder {
        private Person person;

        public void setPerson(Person person) {
            this.person = person;
        }

        public Person getPerson() {
            return person;
        }
    }

    public static class ButtonWithEventHandler extends NativeButton {
        private transient JsInTemplateModel model;

        @ClientDelegate
        public void updateModel() {
            model.setFirstName("ThroughButton");
            model.setItemCount(78);
        }

        public void setModel(JsInTemplateModel model) {
            this.model = model;
        }
    }

    @Id("buttonUpdateModel")
    private ButtonWithEventHandler buttonWithEventHandler;

    @Override
    protected JsInTemplateModel getModel() {
        return (JsInTemplateModel) super.getModel();
    }

    public JsInTemplateView() {
        getModel().setFirstName("Initial");
        getModel().setItemCount(0);

        buttonWithEventHandler.setModel(getModel());
    }

    @ClientDelegate
    public void updateModel() {
        getModel().setFirstName("Another");
        getModel().setItemCount(getModel().getItemCount() == 0 ? 3 : 0);

        getModel().setPerson(new Person("Mr", "Smith", 42));

        PersonHolder personHolder = new PersonHolder();
        personHolder.setPerson(new Person("Mr", "Anderson", 53));
        getModel().setPersonHolder(personHolder);
    }
}
