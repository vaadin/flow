package com.vaadin.humminbird.tutorial.template;

import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.ModelList;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.ui.Template;

@CodeFor("tutorial-template-for.asciidoc")
public class EmployeesTable extends Template {

    public void addEmployee(String name, String title, String email) {
        // create a new item representing an employee in inside the model
        StateNode employeeItem = new StateNode(ModelMap.class);

        // populate the values for the employee item
        ModelMap employeeModel = employeeItem.getFeature(ModelMap.class);
        employeeModel.setValue("name", name);
        employeeModel.setValue("title", title);
        employeeModel.setValue("email", email);

        // add the new item to the list of employees
        getEmployeesList().add(employeeItem);
    }

    // helper for fetching the employees list from the model
    private ModelList getEmployeesList() {
        StateNode employees = (StateNode) getModelMap().getValue("employees");

        // create the employees list if necessary
        if (employees == null) {
            employees = new StateNode(ModelList.class);
            getModelMap().setValue("employees", employees);
        }

        return employees.getFeature(ModelList.class);
    }

    // helper for accessing the model of the template
    private ModelMap getModelMap() {
        return getElement().getNode().getFeature(ModelMap.class);
    }
}
