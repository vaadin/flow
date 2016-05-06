package com.vaadin.humminbird.tutorial.template;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.hummingbird.StateNode;
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
        employeeModel.setValue("title", name);
        employeeModel.setValue("email", name);

        // add the new item to the list of employees
        getEmployeesList().add(employeeItem);
    }

    // helper for fetching the employees list from the model
    private List<StateNode> getEmployeesList() {
        ArrayList<StateNode> employees = (ArrayList<StateNode>) getModel()
                .getValue("employees");

        // create the employees list if necessary
        if (employees == null) {
            employees = new ArrayList<>();
            getModel().setValue("employees", employees);
        }

        return employees;
    }

    // helper for accessing the model of the template
    private ModelMap getModel() {
        return getElement().getNode().getFeature(ModelMap.class);
    }
}