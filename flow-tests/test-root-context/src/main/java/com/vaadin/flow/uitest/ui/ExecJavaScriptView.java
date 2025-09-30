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
package com.vaadin.flow.uitest.ui;

import java.io.Serializable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

import elemental.json.Json;

@Route(value = "com.vaadin.flow.uitest.ui.ExecJavaScriptView", layout = ViewTestLayout.class)
public class ExecJavaScriptView extends AbstractDivView {

    @Override
    protected void onShow() {
        NativeButton alertButton = createJsButton("Alert", "alertButton",
                "window.alert($0)", "Hello world");
        NativeButton focusButton = createJsButton("Focus Alert button",
                "focusButton", "$0.focus()", alertButton);
        NativeButton swapText = createJsButton("Swap button texts",
                "swapButton",
                "(function() {var t = $0.textContent; $0.textContent = $1.textContent; $1.textContent = t;})()",
                alertButton, focusButton);
        NativeButton logButton = createJsButton("Log", "logButton",
                "console.log($0)",
                JacksonUtils.createArray(JacksonUtils.createNode("Hello world"),
                        JacksonUtils.createNode(true)));

        NativeButton elementAwaitButton = createButton("Element await button",
                "elementAwaitButton",
                e -> e.getSource().getElement().executeJs("""
                        const result = new Promise((resolve) => {
                          setTimeout(() => resolve(42), 10);
                        });
                        return await result;
                        """).then(Integer.class, success -> {
                    Span span = new Span();
                    span.setId("elementAwaitResult");
                    span.setText("Element execute JS await result: " + success);
                    add(span);
                }, error -> {
                    Span span = new Span();
                    span.setId("elementAwaitResult");
                    span.setText("Element execute JS await error: " + error);
                    add(span);
                }));

        NativeButton pageAwaitButton = createButton("Page await button",
                "pageAwaitButton", e -> UI.getCurrent().getPage().executeJs("""
                        const result = new Promise((resolve) => {
                          setTimeout(() => resolve(72), 10);
                        });
                        return await result;
                        """).then(Integer.class, success -> {
                    Span span = new Span();
                    span.setId("pageAwaitResult");
                    span.setText("Page execute JS await result: " + success);
                    add(span);
                }, error -> {
                    Span span = new Span();
                    span.setId("pageAwaitResult");
                    span.setText("Page execute JS await error: " + error);
                    add(span);
                }));

        NativeButton createElementButton = createButton(
                "Create and update element", "createButton", e -> {
                    Input input = new Input();
                    input.addClassName("newInput");
                    input.getElement().executeJs("this.value=$0",
                            "Value from js");
                    add(input);
                });

        // Bean serialization tests
        NativeButton simpleBeanButton = createButton("Simple Bean",
                "simpleBeanButton", e -> testSimpleBean());
        NativeButton nestedBeanButton = createButton("Nested Bean",
                "nestedBeanButton", e -> testNestedBean());

        add(alertButton, focusButton, swapText, logButton, createElementButton,
                elementAwaitButton, pageAwaitButton, simpleBeanButton,
                nestedBeanButton);
    }

    private void testSimpleBean() {
        SimpleBean bean = new SimpleBean("TestBean", 42, true);

        UI.getCurrent().getPage().executeJs(
                """
                        const bean = $0;
                        const result = `name=${bean.name}, value=${bean.value}, active=${bean.active}`;

                        const resultDiv = document.createElement('div');
                        resultDiv.id = 'simpleBeanResult';
                        resultDiv.textContent = result;
                        document.body.appendChild(resultDiv);

                        const statusDiv = document.createElement('div');
                        statusDiv.id = 'simpleBeanStatus';
                        statusDiv.textContent = 'Simple bean sent and received';
                        document.body.appendChild(statusDiv);
                        """,
                bean);
    }

    private void testNestedBean() {
        SimpleBean inner = new SimpleBean("Inner", 100, false);
        NestedBean outer = new NestedBean("Outer", inner);

        UI.getCurrent().getPage().executeJs(
                """
                        const bean = $0;
                        const result = `title=${bean.title}, simple.name=${bean.simple.name}, simple.value=${bean.simple.value}`;

                        const resultDiv = document.createElement('div');
                        resultDiv.id = 'nestedBeanResult';
                        resultDiv.textContent = result;
                        document.body.appendChild(resultDiv);

                        const statusDiv = document.createElement('div');
                        statusDiv.id = 'nestedBeanStatus';
                        statusDiv.textContent = 'Nested bean sent and received';
                        document.body.appendChild(statusDiv);
                        """,
                outer);
    }

    public static class SimpleBean {
        public String name;
        public int value;
        public boolean active;

        public SimpleBean(String name, int value, boolean active) {
            this.name = name;
            this.value = value;
            this.active = active;
        }
    }

    public static class NestedBean {
        public String title;
        public SimpleBean simple;

        public NestedBean(String title, SimpleBean simple) {
            this.title = title;
            this.simple = simple;
        }
    }

    private NativeButton createJsButton(String text, String id, String script,
            Serializable... arguments) {
        return createButton(text, id,
                e -> UI.getCurrent().getPage().executeJs(script, arguments));
    }
}
