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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.polymertemplate.RepeatIndex;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.DomRepeatView", layout = ViewTestLayout.class)
@Tag("employees-list")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/DomRepeatPolymerTemplate.html")
@JsModule("DomRepeatPolymerTemplate.js")
public class DomRepeatView
        extends PolymerTemplate<DomRepeatView.EmployeesModel> {
    static final int NUMBER_OF_EMPLOYEES = 3;
    static final String EVENT_INDEX_ID = "eventIndex";
    static final String REPEAT_INDEX_ID = "repeatIndex";
    static final String TR_ID_PREFIX = "name";

    public DomRepeatView() {
        setId("template");
        setEmployees(IntStream.range(0, NUMBER_OF_EMPLOYEES)
                .mapToObj(i -> new DomRepeatView.Employee("name" + i,
                        "title" + i, "email" + i))
                .collect(Collectors.toList()));
    }

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
