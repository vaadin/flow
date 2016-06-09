package com.vaadin.humminbird.tutorial.template;

import java.util.List;

import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.hummingbird.template.model.TemplateModel;
import com.vaadin.ui.Template;

@CodeFor("tutorial-template-for.asciidoc")
public class EmployeesTable extends Template {

    public class Employee {
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
