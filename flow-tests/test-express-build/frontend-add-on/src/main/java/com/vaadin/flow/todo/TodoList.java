/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.todo;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.template.Id;

/**
 * The main application view of the todo application.
 */
@Tag("todo-template")
@JsModule("./TodoTemplate.js")
public class TodoList extends LitTemplate {

    @Id("creator")
    private TodoCreator creator;

    public TodoList() {
        setId("template");

        creator.addCreateCallback(todo -> addNewTodoItem(todo));
    }

    private void addNewTodoItem(Todo todo) {
        TodoElement todoElement = new TodoElement(todo);

        todoElement.getElement().addEventListener("remove",
                e -> getElement().removeChild(todoElement.getElement()));

        todoElement.addStateChangeListener(() -> {
            if (todoElement.isCompleted()) {
                todoElement.getElement().setAttribute("slot", "done");
            }
        });

        getElement().appendChild(todoElement.getElement());
    }

}
