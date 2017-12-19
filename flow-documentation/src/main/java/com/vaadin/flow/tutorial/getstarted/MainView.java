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
package com.vaadin.flow.tutorial.getstarted;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcons;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.tutorial.annotations.CodeFor;

/**
 * The main view contains a button and a template element.
 */
@SuppressWarnings("serial")
@HtmlImport("styles.html")
@Route("")
@CodeFor("get-started.asciidoc")
public class MainView extends VerticalLayout {
    private CustomerService service = CustomerService.getInstance();
    private Grid<Customer> grid = new Grid<>();
    private TextField filterText = new TextField();
    private CustomerForm form = new CustomerForm(this);

    public MainView() {
        filterText.setPlaceholder("filter by name...");
        filterText.addValueChangeListener(e -> updateList());

        Button clearFilterTextBtn = new Button(
                new Icon(VaadinIcons.CLOSE_CIRCLE));
        clearFilterTextBtn.addClickListener(e -> filterText.clear());

        HorizontalLayout filtering = new HorizontalLayout(filterText,
                clearFilterTextBtn);

        Button addCustomerBtn = new Button("Add new customer");
        addCustomerBtn.addClickListener(e -> {
            grid.asSingleSelect().clear();
            form.setCustomer(new Customer());
        });

        HorizontalLayout toolbar = new HorizontalLayout(filtering,
                addCustomerBtn);

        grid.setSizeFull();

        grid.addColumn(Customer::getFirstName).setHeader("First name");
        grid.addColumn(Customer::getLastName).setHeader("Last name");
        grid.addColumn(Customer::getStatus).setHeader("Status");

        HorizontalLayout main = new HorizontalLayout(grid, form);
        main.setAlignItems(Alignment.START);
        main.setSizeFull();

        add(toolbar, main);

        updateList();

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                form.setCustomer(event.getValue());
            } else {
                form.setCustomer(null);
            }
        });

        ExampleTemplate template = new ExampleTemplate();
        Button button = new Button("Click me",
                event -> template.setValue("Clicked!"));
        add(button, template);
        // Add the next two lines:
        // The rest is already there...
        add(grid);
        grid.setItems(service.findAll());
        add(filtering, grid);
        setSizeUndefined();
        add(filtering, main);
    }

    public void updateList() {
        grid.setItems(service.findAll(filterText.getValue()));
    }
}
