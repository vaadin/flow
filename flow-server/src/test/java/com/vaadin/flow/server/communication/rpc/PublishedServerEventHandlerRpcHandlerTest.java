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
package com.vaadin.flow.server.communication.rpc;

import java.util.List;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.AllowInert;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@NotThreadSafe
class PublishedServerEventHandlerRpcHandlerTest {

    private VaadinService service;

    private VaadinSession session;

    @Tag("a")
    public static class ComponentWithCompute extends Component {

        private boolean isInvoked;

        protected void intMethod(int i) {
        }

        @ClientCallable
        private void method() {
            isInvoked = true;
        }

        @ClientCallable
        @AllowInert
        private void methodThatCanBeCalledWhenInert() {
            isInvoked = true;
        }

        @ClientCallable
        private int compute(int input) {
            if (input < 0) {
                throw new ArithmeticException();
            } else {
                return (int) Math.sqrt(input);
            }
        }

    }

    @Tag(Tag.DIV)
    public static class EnabledHandler extends Component {

        private boolean isInvoked;

        @ClientCallable(DisabledUpdateMode.ALWAYS)
        private void operation() {
            isInvoked = true;
        }

    }

    enum Title {
        MR, MRS;
    }

    @Tag(Tag.DIV)
    public static class DecoderParameters extends Component {

        private boolean isInvoked;

        @ClientCallable
        private void method(Long longValue, Title title) {
            isInvoked = true;
        }
    }

    public static class ComponentWithNoClientCallableMethod
            extends ComponentWithCompute {

        /**
         * No {@link ClientCallable} annotation.
         */
        public void operation() {
        }
    }

    public static class CompositeOfComponentWithCompute
            extends Composite<ComponentWithCompute> {
    }

    public static class CompositeOfComposite
            extends Composite<CompositeOfComponentWithCompute> {
    }

    public static class MethodWithVarArgParameter extends ComponentWithCompute {

        private String[] varArg;

        @ClientCallable
        protected void varArgMethod(@EventData("foo") String... args) {
            varArg = args;
        }

    }

    public static class MethodWithParameters extends ComponentWithCompute {

        private int intArg;
        private boolean booleanArg;
        private String strArg;
        private boolean[] arrayArg;
        private Double[] doubleArg;
        private Integer[] varArg;
        private int[][] doubleArray;
        private JsonNode jsonValue;
        private JsonNode jsonNode;

        @Override
        @ClientCallable
        protected void intMethod(@EventData("foo") int i) {
            intArg = i;
        }

        @EventHandler
        protected void booleanMethod(@EventData("foo") boolean value) {
            booleanArg = value;
        }

        @ClientCallable
        protected void method1(@EventData("foo") String str,
                @EventData("bar") boolean[] array) {
            strArg = str;
            arrayArg = array;
        }

        @ClientCallable
        protected void method2(@EventData("foo") Double[] arg1,
                @EventData("bar") Integer... varArg) {
            doubleArg = arg1;
            this.varArg = varArg;
        }

        @ClientCallable
        protected void method3(@EventData("foo") int[][] array) {
            doubleArray = array;
        }

        @ClientCallable
        protected void method4(@EventData("foo") JsonNode value) {
            jsonValue = value;
        }

        @ClientCallable
        protected void method5(@EventData("foo") JsonNode value) {
            jsonNode = value;
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        assertNull(System.getSecurityManager());

        MockServletServiceSessionSetup setup = new MockServletServiceSessionSetup();

        service = setup.getService();

        MockDeploymentConfiguration configuration = setup
                .getDeploymentConfiguration();
        configuration.setProductionMode(false);

        VaadinService.setCurrent(service);

        session = setup.getSession();
        Mockito.when(session.hasLock()).thenReturn(true);
    }

    @AfterEach
    public void tearDown() {
        service = null;
        VaadinService.setCurrent(null);
    }

    @Test
    public void methodIsInvoked() {
        ComponentWithCompute component = new ComponentWithCompute();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method", JacksonUtils.createArrayNode(),
                -1);

        assertTrue(component.isInvoked);
    }

    @Test
    public void methodIsNotInvokedWhenInert() {
        ComponentWithCompute component = new ComponentWithCompute();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method", JacksonUtils.createArrayNode(),
                -1, true);

        assertFalse(component.isInvoked);
    }

    @Test
    public void methodIsInvokedWhenInertAndInertAllowed() {
        ComponentWithCompute component = new ComponentWithCompute();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "methodThatCanBeCalledWhenInert",
                JacksonUtils.createArrayNode(), -1, true);

        assertTrue(component.isInvoked);
    }

    @Test
    public void methodIsInvokedOnCompositeContent() {
        CompositeOfComponentWithCompute composite = new CompositeOfComponentWithCompute();
        ComponentWithCompute component = composite.getContent();
        PublishedServerEventHandlerRpcHandler.invokeMethod(composite,
                composite.getClass(), "method", JacksonUtils.createArrayNode(),
                -1);

        assertTrue(component.isInvoked);
    }

    @Test
    public void methodIsInvokectOnCompositeOfComposite() {
        CompositeOfComposite composite = new CompositeOfComposite();
        ComponentWithCompute component = composite.getContent().getContent();
        PublishedServerEventHandlerRpcHandler.invokeMethod(composite,
                composite.getClass(), "method", JacksonUtils.createArrayNode(),
                -1);

        assertTrue(component.isInvoked);
    }

    @Test
    public void methodWithDecoderParameters_convertableValues_methodIsInvoked() {
        ArrayNode params = JacksonUtils.createArrayNode();
        params.add("264");
        params.add("MRS");

        DecoderParameters component = new DecoderParameters();
        UI ui = new UI();

        ui.getInternals().setSession(session);

        ui.add(component);
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method", params, -1);

        assertTrue(component.isInvoked);
    }

    @Test
    public void methodWithDecoderParameters_nonConvertableValues_methodIsInvoked() {
        assertThrows(IllegalArgumentException.class, () -> {
            ArrayNode params = JacksonUtils.createArrayNode();
            params.add("264.1");
            params.add("MR");

            UI ui = new UI();
            ui.getInternals().setSession(session);

            DecoderParameters component = new DecoderParameters();
            ui.add(component);
            PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                    component.getClass(), "method", params, -1);
        });
    }

    @Test
    public void methodWithoutArgs_argsProvided() {
        assertThrows(IllegalArgumentException.class, () -> {
            ArrayNode args = JacksonUtils.createArrayNode();
            args.add(true);
            ComponentWithCompute component = new ComponentWithCompute();
            PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                    component.getClass(), "method", args, -1);
        });
    }

    @Test
    public void promiseSuccess() {
        int promiseId = 4;

        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(36);

        ComponentWithCompute component = new ComponentWithCompute();
        UI ui = new UI();
        ui.getInternals().setSession(session);
        ui.add(component);

        // Get rid of attach invocations
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().dumpPendingJavaScriptInvocations();

        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "compute", args, promiseId);

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        List<PendingJavaScriptInvocation> pendingJavaScriptInvocations = ui
                .getInternals().dumpPendingJavaScriptInvocations();
        assertEquals(1, pendingJavaScriptInvocations.size());

        JavaScriptInvocation invocation = pendingJavaScriptInvocations.get(0)
                .getInvocation();
        assertTrue(
                invocation.getExpression()
                        .contains(JsonConstants.RPC_PROMISE_CALLBACK_NAME),
                "Invocation does not look like a promise callback");

        List<Object> parameters = invocation.getParameters();
        assertEquals(3, parameters.size(),
                "Expected three paramters: promiseId, value, target");
        assertEquals(Integer.valueOf(promiseId), parameters.get(0),
                "Promise id should match the value passed to invokeMethod");
        assertEquals(Integer.valueOf(6), parameters.get(1),
                "Promise value should be sqrt(36) = 6");
        assertEquals(component.getElement(), parameters.get(2),
                "Target should be the component's element");
    }

    @Test
    public void promiseFailure() {
        int promiseId = 4;

        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(-36);

        ComponentWithCompute component = new ComponentWithCompute();
        UI ui = new UI();
        ui.getInternals().setSession(session);
        ui.add(component);

        // Get rid of attach invocations
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().dumpPendingJavaScriptInvocations();

        try {
            PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                    component.getClass(), "compute", args, promiseId);
            fail("Exception should be thrown");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof ArithmeticException);
        }

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        List<PendingJavaScriptInvocation> pendingJavaScriptInvocations = ui
                .getInternals().dumpPendingJavaScriptInvocations();
        assertEquals(1, pendingJavaScriptInvocations.size());

        JavaScriptInvocation invocation = pendingJavaScriptInvocations.get(0)
                .getInvocation();
        assertTrue(
                invocation.getExpression()
                        .contains(JsonConstants.RPC_PROMISE_CALLBACK_NAME),
                "Invocation does not look like a promise callback");

        List<Object> parameters = invocation.getParameters();
        assertEquals(2, parameters.size(),
                "Expected two paramters: promiseId,  target");
        assertEquals(Integer.valueOf(promiseId), parameters.get(0),
                "Promise id should match the value passed to invokeMethod");
        assertEquals(component.getElement(), parameters.get(1),
                "Target should be the component's element");
    }

    @Test
    public void methodWithVarArg_acceptNoValues() {
        ArrayNode array = JacksonUtils.createArrayNode();

        MethodWithVarArgParameter component = new MethodWithVarArgParameter();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "varArgMethod", array, -1);

        assertEquals(0, component.varArg.length);
    }

    @Test
    public void methodWithJsonValueIsInvoked() {
        ArrayNode array = JacksonUtils.createArrayNode();

        ObjectNode json = JacksonUtils.createObjectNode();
        json.put("foo", "bar");
        array.add(json);

        MethodWithParameters component = new MethodWithParameters();
        UI ui = new MockUI();
        ui.add(component);
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method4", array, -1);

        assertEquals(component.jsonValue, json);
    }

    @Test
    public void methodWithJacksonJsonValueIsInvoked() {
        ArrayNode array = JacksonUtils.createArrayNode();

        ObjectNode json = JacksonUtils.createObjectNode();
        json.put("foo", "bar");
        array.add(json);

        MethodWithParameters component = new MethodWithParameters();
        UI ui = new MockUI();
        ui.add(component);
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method5", array, -1);

        assertEquals(component.jsonNode, json);
    }

    @Test
    public void methodWithSeveralArgsAndVarArg_acceptNoValues() {
        ArrayNode array = JacksonUtils.createArrayNode();

        ArrayNode firstArg = JacksonUtils.createArrayNode();
        firstArg.add(5.6d);
        firstArg.add(78.36d);

        array.add(firstArg);

        MethodWithParameters component = new MethodWithParameters();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method2", array, -1);

        assertArrayEquals(
                new Double[] { firstArg.get(0).doubleValue(),
                        firstArg.get(1).doubleValue() },
                component.doubleArg);

        assertNotNull(component.varArg);
        assertEquals(0, component.varArg.length);
    }

    @Test
    public void methodWithVarArg_acceptOneValue() {
        ArrayNode array = JacksonUtils.createArrayNode();

        array.add("foo");

        MethodWithVarArgParameter component = new MethodWithVarArgParameter();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "varArgMethod", array, -1);

        assertEquals(1, component.varArg.length);
        assertEquals("foo", component.varArg[0]);
    }

    @Test
    public void methodWithVarArg_arrayIsCorrectlyHandled() {
        ArrayNode array = JacksonUtils.createArrayNode();

        ArrayNode value = JacksonUtils.createArrayNode();
        value.add("foo");
        array.add(value);

        MethodWithVarArgParameter component = new MethodWithVarArgParameter();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "varArgMethod", array, -1);

        assertArrayEquals(new String[] { value.get(0).asString() },
                component.varArg);
    }

    @Test
    public void nullValueShouldReturnZeroForPrimitive() {
        ArrayNode array = JacksonUtils.createArrayNode();
        array.add(JacksonUtils.nullNode());
        MethodWithParameters component = new MethodWithParameters();
        component.intArg = -1;
        component.booleanArg = true;

        // Passing null to a primitive parameter should throw an exception
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "intMethod", array, -1);

        // Verify the field was not modified
        assertEquals(0, component.intArg);
    }

    @Test
    public void noClientCallableMethodException() {
        assertThrows(IllegalStateException.class, () -> {
            ComponentWithNoClientCallableMethod component = new ComponentWithNoClientCallableMethod();
            PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                    component.getClass(), "operation",
                    JacksonUtils.createArrayNode(), -1);
        });
    }

    @Test
    public void noMethodException() {
        assertThrows(IllegalStateException.class, () -> {
            ComponentWithNoClientCallableMethod component = new ComponentWithNoClientCallableMethod();
            PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                    component.getClass(), "operation1",
                    JacksonUtils.createArrayNode(), -1);
        });
    }

    @Test
    public void enabledElement_methodIsInvoked() {
        UI ui = new UI();
        ComponentWithCompute component = new ComponentWithCompute();
        ui.add(component);

        requestInvokeMethod(component);

        assertTrue(component.isInvoked);
    }

    @Test
    public void disabledElement_ClientCallableIsNotInvoked() {
        UI ui = new UI();
        ComponentWithCompute component = new ComponentWithCompute();
        ui.add(component);

        component.getElement().setEnabled(false);

        requestInvokeMethod(component);

        assertFalse(component.isInvoked);
    }

    @Test
    public void disabledElement_clientDelegateAllowsRPC_methodIsInvoked() {
        UI ui = new UI();
        EnabledHandler component = new EnabledHandler();
        ui.add(component);

        component.getElement().setEnabled(false);

        assertFalse(component.isInvoked);

        requestInvokeMethod(component, "operation");

        assertTrue(component.isInvoked);
    }

    private void requestInvokeMethod(Component component) {
        requestInvokeMethod(component, "method");
    }

    private void requestInvokeMethod(Component component, String method) {
        ObjectNode json = JacksonUtils.createObjectNode();
        json.put(JsonConstants.RPC_TEMPLATE_EVENT_METHOD_NAME, method);

        new PublishedServerEventHandlerRpcHandler()
                .handleNode(component.getElement().getNode(), json);
    }

}
