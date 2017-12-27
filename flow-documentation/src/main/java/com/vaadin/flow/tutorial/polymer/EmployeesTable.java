package com.vaadin.flow.tutorial.polymer;

import java.util.List;

import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.polymertemplate.RepeatIndex;
import com.vaadin.flow.templatemodel.Include;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.flow.tutorial.polymer.EmployeesTable.EmployeesModel;

@CodeFor("polymer-templates/tutorial-template-list-bindings.asciidoc")
public class EmployeesTable extends PolymerTemplate<EmployeesModel> {

    public class Employee {
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

    public void setEmployees(List<Employee> employees) {
        getModel().setEmployees(employees);
    }

    public List<Employee> getEmployees() {
        return getModel().getEmployees();
    }

    public void updateTitle() {
        getEmployees().forEach(employee -> employee.setTitle("Mr."));
    }

    @EventHandler
    public void processElement(@RepeatIndex int itemIndex) {
        System.out.println(getEmployees().get(itemIndex).getName());
    }
}
