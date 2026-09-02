/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.devloop.test.app.mutable;

import java.time.LocalDate;
import java.util.Locale;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.devloop.test.shared.DueDateFormatter;
import com.vaadin.flow.router.Route;

/**
 * The view the ITs rewrite.
 * <p>
 * Everything on it is here to be observed from the browser after an
 * {@code apply}: {@link #title()} is a method body a hot swap can replace,
 * {@code due-date} is rendered by a class in the sibling module, and the
 * stylesheet it loads lives in that module too. {@code refresh} re-renders
 * without a page reload, which is how a redefine that Flow does not refresh by
 * itself is still checked.
 * <p>
 * In the {@code mutable} package by convention: the ITs edit sources in this
 * package and revert them afterwards, so a failed run never leaves the working
 * tree dirty. Nothing outside this package and the two files the tests name
 * explicitly is ever touched.
 * <p>
 * Built from {@code flow-html-components} rather than Vaadin components,
 * because those live outside this repository and the dev loop has no opinion
 * about which components a view uses.
 */
@Route("")
@StyleSheet("task-list.css")
public class TaskListView extends Div {

    private final TaskService taskService;

    private final Span title = new Span();
    private final Span dueDate = new Span();
    private final Span tasks = new Span();

    public TaskListView(TaskService taskService) {
        this.taskService = taskService;
        addClassName("task-list-view");
        title.setId("title");
        dueDate.setId("due-date");
        tasks.setId("tasks");
        NativeButton refresh = new NativeButton("Refresh", event -> render());
        refresh.setId("refresh");
        add(title, dueDate, tasks, refresh);
        render();
    }

    private void render() {
        title.setText(title());
        dueDate.setText(new DueDateFormatter(Locale.ENGLISH)
                .format(LocalDate.of(2026, 1, 31)));
        tasks.setText(String.join(", ", taskService.list()));
    }

    /** A method body, which is what a stock JVM can hot swap. */
    private String title() {
        return "Task List";
    }
}
