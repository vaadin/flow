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
import com.vaadin.flow.template.angular.model.TemplateModel;
import com.vaadin.ui.AngularTemplate;

@Route(value = "com.vaadin.flow.uitest.ui.template.FormView", layout = ViewTestLayout.class)
public class FormView extends AngularTemplate {

    static final String ID_FIRST_NAME = "firstName";
    static final String ID_LAST_NAME = "lastName";
    static final String ID_AGE = "age";

    public interface ReadonlyFormModel extends TemplateModel {
        void setPerson(Person person);

        Person getPerson();
    }

    @Override
    protected ReadonlyFormModel getModel() {
        return (ReadonlyFormModel) super.getModel();
    }

    public FormView() {
        Person person = new Person("Hello", "World", 32);

        getModel().setPerson(person);
    }

    @ClientDelegate
    public void updateModel() {
        Person p = getModel().getPerson();
        p.setFirstName(p.getFirstName() + "!");
        p.setLastName(p.getLastName() + "?");
        p.setAge(p.getAge() + 1);
    }
}
