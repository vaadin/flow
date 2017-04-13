package com.vaadin.flow.tutorial.template;

import java.util.List;

import com.vaadin.annotations.Include;
import com.vaadin.flow.template.angular.model.TemplateModel;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.ui.AngularTemplate;

@Deprecated
@CodeFor("deprecated/tutorial-template-for.asciidoc")
public class EmployeesTable extends AngularTemplate {

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

    @Override
    protected EmployeesModel getModel() {
        return (EmployeesModel) super.getModel();
    }

    public void setEmployees(List<Employee> employees) {
        getModel().setEmployees(employees);
    }

    public List<Employee> getEmployees() {
        return getModel().getEmployees();
    }

    public void updateTitle() {
        getEmployees().stream().forEach(employee -> employee.setTitle("Mr."));
    }
}
