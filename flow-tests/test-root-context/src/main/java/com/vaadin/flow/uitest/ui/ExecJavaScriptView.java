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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

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

        NativeButton beanButton = createButton("Bean Serialization",
                "beanButton", e -> testBeanSerialization());

        NativeButton returnBeanButton = createButton("Return Bean",
                "returnBeanButton", e -> {
                    UI.getCurrent().getPage().executeJs(
                            "return {title: 'ReturnedNested', simple: {name: 'InnerReturned', value: 777, active: true}}")
                            .then(NestedBean.class, bean -> {
                                Div result = new Div();
                                result.setId("returnBeanResult");
                                result.setText("Returned: title=" + bean.title
                                        + ", simple.name=" + bean.simple.name
                                        + ", simple.value=" + bean.simple.value
                                        + ", simple.active="
                                        + bean.simple.active);
                                add(result);

                                Div status = new Div();
                                status.setId("returnBeanStatus");
                                status.setText("Bean returned");
                                add(status);
                            });
                });

        NativeButton listButton = createButton("List Serialization",
                "listButton", e -> testListSerialization());

        NativeButton returnListButton = createButton("Return List",
                "returnListButton", e -> {
                    UI.getCurrent().getPage().executeJs(
                            "return [{name: 'First', value: 1, active: true}, {name: 'Second', value: 2, active: false}]")
                            .then(List.class, list -> {
                                Div result = new Div();
                                result.setId("returnListResult");
                                result.setText("Returned list with "
                                        + list.size() + " items");
                                add(result);

                                Div status = new Div();
                                status.setId("returnListStatus");
                                status.setText("List returned");
                                add(status);
                            });
                });

        NativeButton mapButton = createButton("Map Serialization", "mapButton",
                e -> testMapSerialization());

        NativeButton returnMapButton = createButton("Return Map",
                "returnMapButton", e -> {
                    UI.getCurrent().getPage().executeJs(
                            "return {alpha: {name: 'AlphaBean', value: 111, active: true}, beta: {name: 'BetaBean', value: 222, active: false}}")
                            .then(Map.class, map -> {
                                Div result = new Div();
                                result.setId("returnMapResult");
                                result.setText("Returned map with " + map.size()
                                        + " entries");
                                add(result);

                                Div status = new Div();
                                status.setId("returnMapStatus");
                                status.setText("Map returned");
                                add(status);
                            });
                });

        NativeButton componentArrayButton = createButton("Component Array",
                "componentArrayButton", e -> testComponentArraySerialization());

        NativeButton beanWithComponentButton = createButton(
                "Bean With Component", "beanWithComponentButton",
                e -> testBeanWithComponentSerialization());

        // @ClientCallable integration tests
        NativeButton clientCallableBeanButton = createButton(
                "ClientCallable Bean", "clientCallableBeanButton", e -> {
                    // Test @ClientCallable with bean parameter
                    SimpleBean testBean = new SimpleBean("ClientCallableTest",
                            99, true);
                    getElement().executeJs(
                            "this.$server.handleClientCallableBean($0)",
                            testBean);
                });

        NativeButton clientCallableListButton = createButton(
                "ClientCallable List", "clientCallableListButton", e -> {
                    // Test @ClientCallable with list parameter
                    List<SimpleBean> testList = Arrays.asList(
                            new SimpleBean("Item1", 111, true),
                            new SimpleBean("Item2", 222, false));
                    getElement().executeJs(
                            "this.$server.handleClientCallableList($0)",
                            testList);
                });

        NativeButton clientCallableNestedButton = createButton(
                "ClientCallable Nested", "clientCallableNestedButton", e -> {
                    // Test @ClientCallable with nested bean parameter
                    NestedBean nested = new NestedBean("ClientCallableNested",
                            new SimpleBean("NestedInner", 333, false));
                    getElement().executeJs(
                            "this.$server.handleClientCallableNested($0)",
                            nested);
                });

        add(alertButton, focusButton, swapText, logButton, createElementButton,
                elementAwaitButton, pageAwaitButton, beanButton,
                returnBeanButton, listButton, returnListButton, mapButton,
                returnMapButton, componentArrayButton, beanWithComponentButton,
                clientCallableBeanButton, clientCallableListButton,
                clientCallableNestedButton);
    }

    private void testBeanSerialization() {
        SimpleBean simple = new SimpleBean("TestBean", 42, true);
        SimpleBean inner = new SimpleBean("Inner", 100, false);
        NestedBean nested = new NestedBean("Outer", inner);

        UI.getCurrent().getPage().executeJs(
                """
                        const simpleBean = $0;
                        const nestedBean = $1;

                        const simpleResult = `simple: name=${simpleBean.name}, value=${simpleBean.value}, active=${simpleBean.active}`;
                        const nestedResult = `nested: title=${nestedBean.title}, inner.name=${nestedBean.simple.name}, inner.value=${nestedBean.simple.value}`;

                        const resultDiv = document.createElement('div');
                        resultDiv.id = 'beanResult';
                        resultDiv.textContent = simpleResult + ' | ' + nestedResult;
                        document.body.appendChild(resultDiv);

                        const statusDiv = document.createElement('div');
                        statusDiv.id = 'beanStatus';
                        statusDiv.textContent = 'Bean serialization completed';
                        document.body.appendChild(statusDiv);
                        """,
                simple, nested);
    }

    private void testListSerialization() {
        List<SimpleBean> beanList = Arrays.asList(
                new SimpleBean("FirstItem", 10, true),
                new SimpleBean("SecondItem", 20, false),
                new SimpleBean("ThirdItem", 30, true));

        UI.getCurrent().getPage().executeJs(
                """
                        const beanArray = $0;
                        let result = 'List: ';
                        for (let i = 0; i < beanArray.length; i++) {
                            const bean = beanArray[i];
                            result += `[${i}]: name=${bean.name}, value=${bean.value}, active=${bean.active}`;
                            if (i < beanArray.length - 1) result += ' | ';
                        }

                        const resultDiv = document.createElement('div');
                        resultDiv.id = 'listResult';
                        resultDiv.textContent = result;
                        document.body.appendChild(resultDiv);

                        const statusDiv = document.createElement('div');
                        statusDiv.id = 'listStatus';
                        statusDiv.textContent = 'List serialization completed';
                        document.body.appendChild(statusDiv);
                        """,
                beanList);
    }

    private void testMapSerialization() {
        Map<String, SimpleBean> beanMap = new HashMap<>();
        beanMap.put("first", new SimpleBean("FirstKey", 100, true));
        beanMap.put("second", new SimpleBean("SecondKey", 200, false));
        beanMap.put("third", new SimpleBean("ThirdKey", 300, true));

        UI.getCurrent().getPage().executeJs(
                """
                        const beanMap = $0;
                        let result = 'Map: ';
                        const keys = Object.keys(beanMap);
                        for (let i = 0; i < keys.length; i++) {
                            const key = keys[i];
                            const bean = beanMap[key];
                            result += `${key}: name=${bean.name}, value=${bean.value}, active=${bean.active}`;
                            if (i < keys.length - 1) result += ' | ';
                        }

                        const resultDiv = document.createElement('div');
                        resultDiv.id = 'mapResult';
                        resultDiv.textContent = result;
                        document.body.appendChild(resultDiv);

                        const statusDiv = document.createElement('div');
                        statusDiv.id = 'mapStatus';
                        statusDiv.textContent = 'Map serialization completed';
                        document.body.appendChild(statusDiv);
                        """,
                beanMap);
    }

    private void testComponentArraySerialization() {
        TestComponent component1 = new TestComponent("Component 1");
        TestComponent component2 = new TestComponent("Component 2");

        // Add components to the page so they are attached
        add(component1, component2);

        TestComponent[] componentArray = new TestComponent[] { component1,
                component2 };

        Object[] parameter = new Object[] { componentArray };
        UI.getCurrent().getPage().executeJs(
                """
                        const componentArray = $0;
                        let result = 'Component Array: ';
                        for (let i = 0; i < componentArray.length; i++) {
                            const comp = componentArray[i];
                            if (comp && typeof comp === 'object') {
                                result += `[${i}]: component object`;
                            } else {
                                result += `[${i}]: ${comp}`;
                            }
                            if (i < componentArray.length - 1) result += ' | ';
                        }

                        const resultDiv = document.createElement('div');
                        resultDiv.id = 'componentArrayResult';
                        resultDiv.textContent = result;
                        document.body.appendChild(resultDiv);

                        const statusDiv = document.createElement('div');
                        statusDiv.id = 'componentArrayStatus';
                        statusDiv.textContent = 'Component array serialization completed';
                        document.body.appendChild(statusDiv);
                        """,
                parameter);
    }

    private void testBeanWithComponentSerialization() {
        TestComponent component = new TestComponent("Bean Component");

        // Add component to the page so it is attached
        add(component);

        BeanWithComponent bean = new BeanWithComponent("TestBeanComponent",
                component, 123);

        UI.getCurrent().getPage().executeJs(
                """
                        const bean = $0;
                        let result = `Bean: name=${bean.name}, value=${bean.value}`;
                        if (bean.component && typeof bean.component === 'object') {
                            result += ', component=object';
                        } else {
                            result += `, component=${bean.component}`;
                        }

                        const resultDiv = document.createElement('div');
                        resultDiv.id = 'beanWithComponentResult';
                        resultDiv.textContent = result;
                        document.body.appendChild(resultDiv);

                        const statusDiv = document.createElement('div');
                        statusDiv.id = 'beanWithComponentStatus';
                        statusDiv.textContent = 'Bean with component serialization completed';
                        document.body.appendChild(statusDiv);
                        """,
                bean);
    }

    public static class TestComponent extends Div {
        public TestComponent(String text) {
            setText(text);
        }
    }

    public static class BeanWithComponent {
        public String name;
        public TestComponent component;
        public int value;

        public BeanWithComponent() {
        }

        public BeanWithComponent(String name, TestComponent component,
                int value) {
            this.name = name;
            this.component = component;
            this.value = value;
        }
    }

    @ClientCallable
    public void handleClientCallableBean(SimpleBean bean) {
        Div result = new Div();
        result.setId("clientCallableBeanResult");
        result.setText("ClientCallable Bean: name=" + bean.name + ", value="
                + bean.value + ", active=" + bean.active);
        add(result);

        Div status = new Div();
        status.setId("clientCallableBeanStatus");
        status.setText("ClientCallable bean handled");
        add(status);
    }

    @ClientCallable
    public void handleClientCallableList(List<SimpleBean> beanList) {
        Div result = new Div();
        result.setId("clientCallableListResult");
        StringBuilder text = new StringBuilder("ClientCallable List: ");
        for (int i = 0; i < beanList.size(); i++) {
            SimpleBean bean = beanList.get(i);
            text.append("[").append(i).append("]: name=").append(bean.name)
                    .append(", value=").append(bean.value).append(", active=")
                    .append(bean.active);
            if (i < beanList.size() - 1)
                text.append(" | ");
        }
        result.setText(text.toString());
        add(result);

        Div status = new Div();
        status.setId("clientCallableListStatus");
        status.setText("ClientCallable list handled");
        add(status);
    }

    @ClientCallable
    public void handleClientCallableNested(NestedBean nested) {
        Div result = new Div();
        result.setId("clientCallableNestedResult");
        result.setText("ClientCallable Nested: title=" + nested.title
                + ", simple.name=" + nested.simple.name + ", simple.value="
                + nested.simple.value + ", simple.active="
                + nested.simple.active);
        add(result);

        Div status = new Div();
        status.setId("clientCallableNestedStatus");
        status.setText("ClientCallable nested bean handled");
        add(status);
    }

    public static class SimpleBean {
        public String name;
        public int value;
        public boolean active;

        public SimpleBean() {
        }

        public SimpleBean(String name, int value, boolean active) {
            this.name = name;
            this.value = value;
            this.active = active;
        }
    }

    public static class NestedBean {
        public String title;
        public SimpleBean simple;

        public NestedBean() {
        }

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
