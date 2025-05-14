/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.todo;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;

/**
 * An element for displaying the Todo item. Task can be edited and completion of
 * task can be set in the element.
 */
@Tag("todo-element")
@JsModule("./TodoElement.js")
public class TodoElement extends LitTemplate {

    private List<Runnable> changeListeners = new ArrayList<>(0);

    private Todo todo;

    /**
     * Todo element constructor.
     *
     * @param todo
     *            todo item for this element
     */
    public TodoElement(Todo todo) {
        this.todo = todo;

        populateModel(todo);
        addChangeListeners(todo);
    }

    private void populateModel(Todo todo) {
        getElement().setProperty("task", todo.getTask());
        getElement().setProperty("user", todo.getUser());
        getElement().setProperty("rid", todo.getRid());
        getElement().setProperty("time", todo.getTime()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm")));
        getElement().setProperty("completed", todo.isCompleted());
    }

    private void addChangeListeners(Todo todo) {
        getElement().addPropertyChangeListener("completed",
                event -> taskCompleted());
        getElement().addPropertyChangeListener("task",
                event -> todo.setTask(getElement().getAttribute("task")));
    }

    private void taskCompleted() {
        todo.setCompleted(isCompleted());

        changeListeners.forEach(Runnable::run);
    }

    /**
     * Get the {@link Todo} item for this TodoElement.
     *
     * @return todo item
     */
    public Todo getTodo() {
        return todo;
    }

    /**
     * Returns completion state of this {@link Todo} item.
     *
     * @return todo item completion status
     */
    public boolean isCompleted() {
        return Boolean.parseBoolean(getElement().getAttribute("completed"));
    }

    /**
     * Add a state change listener that is informed when the completed state
     * changes.
     *
     * @param listener
     *            runnable method to be used as a listener
     */
    public void addStateChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }
}
