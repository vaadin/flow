package com.vaadin.flow.frontend;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.todo.TodoList;

@Route(value = "com.vaadin.flow.frontend.TodoView")
public class TodoView extends Div {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        add(new TodoList());
    }

}
