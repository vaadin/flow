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

package com.vaadin.hummingbird.uitest.ui.template;

import java.util.List;

import com.vaadin.annotations.EventData;
import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.RepeatIndex;
import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.template.PolymerTemplate;
import com.vaadin.hummingbird.template.model.TemplateModel;

/**
 * @author Vaadin Ltd.
 */
@Tag("employees-list")
@HtmlImport("/com/vaadin/hummingbird/uitest/ui/template/DomRepeatPolymerTemplate.html")
public class DomRepeatPolymerTemplate
        extends PolymerTemplate<DomRepeatPolymerTemplate.EmployeesModel> {

    public static class Employee {
        private String name;
        private String title;
        private String email;

        public Employee(String name, String title, String email) {
            this.name = name;
            this.title = title;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public String getTitle() {
            return title;
        }

        public String getEmail() {
            return email;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public interface EmployeesModel extends TemplateModel {
        void setEmployees(List<Employee> employees);

        void setEventIndex(int eventIndex);

        void setRepeatIndex(int repeatIndex);
    }

    @EventHandler
    public void handleClick(@EventData("event.model.index") int eventIndex,
            @RepeatIndex int repeatIndex) {
        getModel().setEventIndex(eventIndex);
        getModel().setRepeatIndex(repeatIndex);
    }

    public void setEmployees(List<Employee> employees) {
        getModel().setEmployees(employees);
    }
}
