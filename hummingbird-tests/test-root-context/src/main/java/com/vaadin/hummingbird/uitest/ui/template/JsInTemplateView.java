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
package com.vaadin.hummingbird.uitest.ui.template;

import com.vaadin.annotations.EventHandler;
import com.vaadin.hummingbird.template.model.TemplateModel;
import com.vaadin.ui.Template;

public class JsInTemplateView extends Template {
    public interface JsInTemplateModel extends TemplateModel {
        public String getFirstName();

        public void setFirstName(String firstName);

        public int getItemCount();

        public void setItemCount(int itemCount);

        public void setPerson(Person person);
    }

    @Override
    protected JsInTemplateModel getModel() {
        return (JsInTemplateModel) super.getModel();
    }

    public JsInTemplateView() {
        getModel().setFirstName("Initial");
        getModel().setItemCount(0);
    }

    @EventHandler
    public void updateModel() {
        getModel().setFirstName("Another");
        getModel().setItemCount(getModel().getItemCount() == 0 ? 3 : 0);

        getModel().setPerson(new Person("Mr", "Smith", 42));
    }
}
