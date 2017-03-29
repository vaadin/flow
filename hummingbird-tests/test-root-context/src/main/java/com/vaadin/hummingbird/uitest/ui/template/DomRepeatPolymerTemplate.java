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
import com.vaadin.annotations.Include;
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
        private long id;

        public Employee(String name, String title, String email, long id) {
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

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }
    }

    public interface EmployeesModel extends TemplateModel {
        @Include({ "name", "title", "email" })
        void setEmployees(List<Employee> employees);

        List<Employee> getEmployees();
    }

    @EventHandler
    public void handleClick(@EventData("event.model.index") Integer index) {
        System.out.println("old");
        System.out.println(index);
    }

    @EventHandler
    public void handleClick2(@RepeatIndex int index) {
        System.out.println("new");
        System.out.println(index);
    }

    public void setEmployees(List<Employee> employees) {
        getModel().setEmployees(employees);
    }

    public List<Employee> getEmployees() {
        return getModel().getEmployees();
    }

    public void updateTitle() {
        getEmployees().forEach(employee -> employee.setTitle("Mr."));
    }
}
