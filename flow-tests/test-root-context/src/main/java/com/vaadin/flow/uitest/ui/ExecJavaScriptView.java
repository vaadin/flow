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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
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

        add(alertButton, focusButton, swapText, logButton, createElementButton,
                elementAwaitButton, pageAwaitButton);

        // Add bean serialization test buttons
        addBeanSerializationTests();
    }

    private NativeButton createJsButton(String text, String id, String script,
            Serializable... arguments) {
        return createButton(text, id,
                e -> UI.getCurrent().getPage().executeJs(script, arguments));
    }

    // Bean classes for testing
    public static class SimpleBean implements Serializable {
        public String name;
        public int value;
        public boolean active;

        public SimpleBean(String name, int value, boolean active) {
            this.name = name;
            this.value = value;
            this.active = active;
        }
    }

    public static class NestedBean implements Serializable {
        public String title;
        public SimpleBean simple;

        public NestedBean(String title, SimpleBean simple) {
            this.title = title;
            this.simple = simple;
        }
    }

    public static class BeanWithComponent implements Serializable {
        public String label;
        public Component button;
        public NestedBeanWithComponent nested;

        public BeanWithComponent(String label, Component button,
                NestedBeanWithComponent nested) {
            this.label = label;
            this.button = button;
            this.nested = nested;
        }
    }

    public static class NestedBeanWithComponent implements Serializable {
        public String description;
        public Component div;

        public NestedBeanWithComponent(String description, Component div) {
            this.description = description;
            this.div = div;
        }
    }

    private void addBeanSerializationTests() {
        // Test 1: Simple bean with only primitive types
        NativeButton simpleBeanButton = createButton("Test Simple Bean",
                "simpleBeanButton", e -> {
                    SimpleBean bean = new SimpleBean("TestBean", 42, true);

                    e.getSource().getElement().executeJs("const bean = $0; "
                            + "const result = document.createElement('div'); "
                            + "result.id = 'simpleBeanResult'; "
                            + "result.textContent = 'name=' + bean.name + ', value=' + bean.value + ', active=' + bean.active; "
                            + "document.body.appendChild(result); "
                            + "const status = document.createElement('span'); "
                            + "status.id = 'simpleBeanStatus'; "
                            + "status.textContent = 'Simple bean sent and received'; "
                            + "document.body.appendChild(status);", bean);
                });

        // Test 2: Nested beans
        NativeButton nestedBeanButton = createButton("Test Nested Bean",
                "nestedBeanButton", e -> {
                    SimpleBean simple = new SimpleBean("Inner", 100, false);
                    NestedBean nested = new NestedBean("Outer", simple);

                    e.getSource().getElement().executeJs("const bean = $0; "
                            + "const result = document.createElement('div'); "
                            + "result.id = 'nestedBeanResult'; "
                            + "result.textContent = 'title=' + bean.title + ', simple.name=' + bean.simple.name + ', simple.value=' + bean.simple.value; "
                            + "document.body.appendChild(result); "
                            + "const status = document.createElement('span'); "
                            + "status.id = 'nestedBeanStatus'; "
                            + "status.textContent = 'Nested bean sent and received'; "
                            + "document.body.appendChild(status);", nested);
                });

        // Test 3: Bean with component references
        NativeButton componentBeanButton = createButton("Test Component Bean",
                "componentBeanButton", e -> {
                    // Create components that will be referenced in the bean
                    NativeButton testButton = new NativeButton("Bean Button");
                    testButton.setId("beanButton");
                    add(testButton);

                    Div testDiv = new Div("Bean Div");
                    testDiv.setId("beanDiv");
                    add(testDiv);

                    NestedBeanWithComponent nestedWithComp = new NestedBeanWithComponent(
                            "Nested with component", testDiv);
                    BeanWithComponent beanWithComp = new BeanWithComponent(
                            "Main bean", testButton, nestedWithComp);

                    e.getSource().getElement().executeJs("const bean = $0; "
                            + "const result = document.createElement('div'); "
                            + "result.id = 'componentBeanResult'; "
                            + "let text = 'label=' + bean.label; "
                            + "if (bean.button && bean.button.tagName) { "
                            + "  text += ', button.tag=' + bean.button.tagName.toLowerCase(); "
                            + "  text += ', button.text=' + bean.button.textContent; "
                            + "} "
                            + "if (bean.nested && bean.nested.description) { "
                            + "  text += ', nested.desc=' + bean.nested.description; "
                            + "  if (bean.nested.div && bean.nested.div.tagName) { "
                            + "    text += ', nested.div.tag=' + bean.nested.div.tagName.toLowerCase(); "
                            + "    text += ', nested.div.text=' + bean.nested.div.textContent; "
                            + "  } " + "} " + "result.textContent = text; "
                            + "document.body.appendChild(result); "
                            + "const status = document.createElement('span'); "
                            + "status.id = 'componentBeanStatus'; "
                            + "status.textContent = 'Component bean sent and received'; "
                            + "document.body.appendChild(status);",
                            beanWithComp);
                });

        add(simpleBeanButton, nestedBeanButton, componentBeanButton);
    }
}
