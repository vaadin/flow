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
package com.vaadin.flow.server.communication.rpc;

import java.util.Arrays;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.tests.util.MockUI;

/**
 * Tests for @ClientCallable method support with bean and collection parameters
 * and return values.
 */
public class ClientCallableBeanSupportTest {

    private MockServletServiceSessionSetup mocks;
    private UI ui;
    private PublishedServerEventHandlerRpcHandler handler;

    // Test bean classes
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

    // Test component with @ClientCallable methods
    @Tag(Tag.DIV)
    public static class ComponentWithClientCallableMethods extends Component {
        private SimpleBean receivedBean;
        private List<SimpleBean> receivedList;
        private NestedBean receivedNestedBean;
        private List<Integer> receivedIntegerList;

        @ClientCallable
        public void handleSimpleBean(@EventData("bean") SimpleBean bean) {
            this.receivedBean = bean;
        }

        @ClientCallable
        public void handleBeanList(@EventData("list") List<SimpleBean> list) {
            this.receivedList = list;
        }

        @ClientCallable
        public void handleNestedBean(@EventData("nested") NestedBean nested) {
            this.receivedNestedBean = nested;
        }

        @ClientCallable
        public void handleIntegerList(
                @EventData("integers") List<Integer> integers) {
            this.receivedIntegerList = integers;
        }

        @ClientCallable
        public SimpleBean returnSimpleBean() {
            return new SimpleBean("returned", 42, true);
        }

        @ClientCallable
        public List<SimpleBean> returnBeanList() {
            return Arrays.asList(new SimpleBean("first", 1, true),
                    new SimpleBean("second", 2, false));
        }

        @ClientCallable
        public NestedBean returnNestedBean() {
            return new NestedBean("outer", new SimpleBean("inner", 100, false));
        }

        @ClientCallable
        public List<Integer> returnIntegerList() {
            return Arrays.asList(10, 20, 30);
        }

        // Getters for test verification
        public SimpleBean getReceivedBean() {
            return receivedBean;
        }

        public List<SimpleBean> getReceivedList() {
            return receivedList;
        }

        public NestedBean getReceivedNestedBean() {
            return receivedNestedBean;
        }

        public List<Integer> getReceivedIntegerList() {
            return receivedIntegerList;
        }
    }

    @Before
    public void setUp() throws Exception {
        mocks = new MockServletServiceSessionSetup();
        VaadinService service = mocks.getService();
        service.init();

        ui = new MockUI();
        VaadinSession.setCurrent(mocks.getSession());
        UI.setCurrent(ui);

        handler = new PublishedServerEventHandlerRpcHandler();
    }

    @After
    public void tearDown() {
        mocks.cleanup();
    }

    @Test
    public void testSimpleBeanParameter() throws Exception {
        ComponentWithClientCallableMethods component = new ComponentWithClientCallableMethods();
        ui.add(component);

        // Create JSON for SimpleBean parameter
        ObjectNode beanJson = JacksonUtils.createObjectNode();
        beanJson.put("name", "TestBean");
        beanJson.put("value", 123);
        beanJson.put("active", true);

        // Create parameters array
        ArrayNode params = JacksonUtils.createArrayNode();
        params.add(beanJson);

        // Invoke the method directly
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "handleSimpleBean", params, -1);

        // Verify the bean was properly deserialized
        SimpleBean received = component.getReceivedBean();
        Assert.assertNotNull("Bean should be received", received);
        Assert.assertEquals("TestBean", received.name);
        Assert.assertEquals(123, received.value);
        Assert.assertTrue(received.active);
    }

    @Test
    public void testBeanListParameter() throws Exception {
        ComponentWithClientCallableMethods component = new ComponentWithClientCallableMethods();
        ui.add(component);

        // Create JSON array with bean objects
        ArrayNode beanArray = JacksonUtils.createArrayNode();

        ObjectNode bean1 = JacksonUtils.createObjectNode();
        bean1.put("name", "First");
        bean1.put("value", 1);
        bean1.put("active", true);
        beanArray.add(bean1);

        ObjectNode bean2 = JacksonUtils.createObjectNode();
        bean2.put("name", "Second");
        bean2.put("value", 2);
        bean2.put("active", false);
        beanArray.add(bean2);

        // Create parameters array
        ArrayNode params = JacksonUtils.createArrayNode();
        params.add(beanArray);

        // Invoke the method directly
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "handleBeanList", params, -1);

        // Verify the list was properly deserialized
        List<SimpleBean> received = component.getReceivedList();
        Assert.assertNotNull("List should be received", received);
        Assert.assertEquals("Should have 2 beans", 2, received.size());

        Assert.assertEquals("First", received.get(0).name);
        Assert.assertEquals(1, received.get(0).value);
        Assert.assertTrue(received.get(0).active);

        Assert.assertEquals("Second", received.get(1).name);
        Assert.assertEquals(2, received.get(1).value);
        Assert.assertFalse(received.get(1).active);
    }

    @Test
    public void testNestedBeanParameter() throws Exception {
        ComponentWithClientCallableMethods component = new ComponentWithClientCallableMethods();
        ui.add(component);

        // Create JSON for nested bean
        ObjectNode innerBean = JacksonUtils.createObjectNode();
        innerBean.put("name", "InnerBean");
        innerBean.put("value", 456);
        innerBean.put("active", false);

        ObjectNode nestedBean = JacksonUtils.createObjectNode();
        nestedBean.put("title", "OuterBean");
        nestedBean.set("simple", innerBean);

        // Create parameters array
        ArrayNode params = JacksonUtils.createArrayNode();
        params.add(nestedBean);

        // Invoke the method directly
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "handleNestedBean", params, -1);

        // Verify the nested bean was properly deserialized
        NestedBean received = component.getReceivedNestedBean();
        Assert.assertNotNull("Nested bean should be received", received);
        Assert.assertEquals("OuterBean", received.title);
        Assert.assertNotNull("Inner bean should be present", received.simple);
        Assert.assertEquals("InnerBean", received.simple.name);
        Assert.assertEquals(456, received.simple.value);
        Assert.assertFalse(received.simple.active);
    }

    @Test
    public void testIntegerListParameter() throws Exception {
        ComponentWithClientCallableMethods component = new ComponentWithClientCallableMethods();
        ui.add(component);

        // Create JSON array with integers
        ArrayNode intArray = JacksonUtils.createArrayNode();
        intArray.add(10);
        intArray.add(20);
        intArray.add(30);

        // Create parameters array
        ArrayNode params = JacksonUtils.createArrayNode();
        params.add(intArray);

        // Invoke the method directly
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "handleIntegerList", params, -1);

        // Verify the integer list was properly deserialized
        List<Integer> received = component.getReceivedIntegerList();
        Assert.assertNotNull("Integer list should be received", received);
        Assert.assertEquals("Should have 3 integers", 3, received.size());
        Assert.assertEquals(Integer.valueOf(10), received.get(0));
        Assert.assertEquals(Integer.valueOf(20), received.get(1));
        Assert.assertEquals(Integer.valueOf(30), received.get(2));
    }
}
