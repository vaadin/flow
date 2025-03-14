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

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

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

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

@NotThreadSafe
public class PublishedServerEventHandlerRpcHandlerTest {

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
        private JsonValue jsonValue;
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
        protected void method4(@EventData("foo") JsonValue value) {
            jsonValue = value;
        }

        @ClientCallable
        protected void method5(@EventData("foo") JsonNode value) {
            jsonNode = value;
        }
    }

    @Before
    public void setUp() throws Exception {
        Assert.assertNull(System.getSecurityManager());

        MockServletServiceSessionSetup setup = new MockServletServiceSessionSetup();

        service = setup.getService();

        MockDeploymentConfiguration configuration = setup
                .getDeploymentConfiguration();
        configuration.setProductionMode(false);

        VaadinService.setCurrent(service);

        session = setup.getSession();
        Mockito.when(session.hasLock()).thenReturn(true);
    }

    @After
    public void tearDown() {
        service = null;
        VaadinService.setCurrent(null);
    }

    @Test
    public void methodIsInvoked() {
        ComponentWithCompute component = new ComponentWithCompute();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method", Json.createArray(), -1);

        Assert.assertTrue(component.isInvoked);
    }

    @Test
    public void methodIsNotInvokedWhenInert() {
        ComponentWithCompute component = new ComponentWithCompute();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method", Json.createArray(), -1, true);

        Assert.assertFalse(component.isInvoked);
    }

    @Test
    public void methodIsInvokedWhenInertAndInertAllowed() {
        ComponentWithCompute component = new ComponentWithCompute();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "methodThatCanBeCalledWhenInert",
                Json.createArray(), -1, true);

        Assert.assertTrue(component.isInvoked);
    }

    @Test
    public void methodIsInvokedOnCompositeContent() {
        CompositeOfComponentWithCompute composite = new CompositeOfComponentWithCompute();
        ComponentWithCompute component = composite.getContent();
        PublishedServerEventHandlerRpcHandler.invokeMethod(composite,
                composite.getClass(), "method", Json.createArray(), -1);

        Assert.assertTrue(component.isInvoked);
    }

    @Test
    public void methodIsInvokectOnCompositeOfComposite() {
        CompositeOfComposite composite = new CompositeOfComposite();
        ComponentWithCompute component = composite.getContent().getContent();
        PublishedServerEventHandlerRpcHandler.invokeMethod(composite,
                composite.getClass(), "method", Json.createArray(), -1);

        Assert.assertTrue(component.isInvoked);
    }

    @Test
    public void methodWithDecoderParameters_convertableValues_methodIsInvoked() {
        JsonArray params = Json.createArray();
        params.set(0, "264");
        params.set(1, "MRS");

        DecoderParameters component = new DecoderParameters();
        UI ui = new UI();

        ui.getInternals().setSession(session);

        ui.add(component);
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method", params, -1);

        Assert.assertTrue(component.isInvoked);
    }

    @Test(expected = IllegalArgumentException.class)
    public void methodWithDecoderParameters_nonConvertableValues_methodIsInvoked() {
        JsonArray params = Json.createArray();
        params.set(0, "264.1");
        params.set(1, "MR");

        UI ui = new UI();
        ui.getInternals().setSession(session);

        DecoderParameters component = new DecoderParameters();
        ui.add(component);
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method", params, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void methodWithoutArgs_argsProvided() {
        JsonArray args = Json.createArray();
        args.set(0, true);
        ComponentWithCompute component = new ComponentWithCompute();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method", args, -1);
    }

    @Test
    public void promiseSuccess() {
        int promiseId = 4;

        JsonArray args = Json.createArray();
        args.set(0, 36);

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
        Assert.assertEquals(1, pendingJavaScriptInvocations.size());

        JavaScriptInvocation invocation = pendingJavaScriptInvocations.get(0)
                .getInvocation();
        Assert.assertTrue("Invocation does not look like a promise callback",
                invocation.getExpression()
                        .contains(JsonConstants.RPC_PROMISE_CALLBACK_NAME));

        List<Object> parameters = invocation.getParameters();
        Assert.assertEquals(
                "Expected three paramters: promiseId, value, target", 3,
                parameters.size());
        Assert.assertEquals(
                "Promise id should match the value passed to invokeMethod",
                Integer.valueOf(promiseId), parameters.get(0));
        Assert.assertEquals("Promise value should be sqrt(36) = 6",
                Integer.valueOf(6), parameters.get(1));
        Assert.assertEquals("Target should be the component's element",
                component.getElement(), parameters.get(2));
    }

    @Test
    public void promiseFailure() {
        int promiseId = 4;

        JsonArray args = Json.createArray();
        args.set(0, -36);

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
            Assert.fail("Exception should be thrown");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getCause() instanceof ArithmeticException);
        }

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        List<PendingJavaScriptInvocation> pendingJavaScriptInvocations = ui
                .getInternals().dumpPendingJavaScriptInvocations();
        Assert.assertEquals(1, pendingJavaScriptInvocations.size());

        JavaScriptInvocation invocation = pendingJavaScriptInvocations.get(0)
                .getInvocation();
        Assert.assertTrue("Invocation does not look like a promise callback",
                invocation.getExpression()
                        .contains(JsonConstants.RPC_PROMISE_CALLBACK_NAME));

        List<Object> parameters = invocation.getParameters();
        Assert.assertEquals("Expected two paramters: promiseId,  target", 2,
                parameters.size());
        Assert.assertEquals(
                "Promise id should match the value passed to invokeMethod",
                Integer.valueOf(promiseId), parameters.get(0));
        Assert.assertEquals("Target should be the component's element",
                component.getElement(), parameters.get(1));
    }

    @Test
    public void methodWithVarArg_acceptNoValues() {
        JsonArray array = Json.createArray();

        MethodWithVarArgParameter component = new MethodWithVarArgParameter();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "varArgMethod", array, -1);

        Assert.assertEquals(0, component.varArg.length);
    }

    @Test
    public void methodWithJsonValueIsInvoked() {
        JsonArray array = Json.createArray();

        JsonObject json = Json.createObject();
        json.put("foo", "bar");
        array.set(0, json);

        MethodWithParameters component = new MethodWithParameters();
        UI ui = new MockUI();
        ui.add(component);
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method4", array, -1);

        Assert.assertEquals(component.jsonValue, json);
    }

    @Test
    public void methodWithJacksonJsonValueIsInvoked() {
        JsonArray array = Json.createArray();

        JsonObject json = Json.createObject();
        json.put("foo", "bar");
        array.set(0, json);

        MethodWithParameters component = new MethodWithParameters();
        UI ui = new MockUI();
        ui.add(component);
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method5", array, -1);

        Assert.assertEquals(component.jsonNode,
                JacksonUtils.mapElemental(json));
    }

    @Test
    public void methodWithSeveralArgsAndVarArg_acceptNoValues() {
        JsonArray array = Json.createArray();

        JsonArray firstArg = Json.createArray();
        firstArg.set(0, 5.6d);
        firstArg.set(1, 78.36d);

        array.set(0, firstArg);

        MethodWithParameters component = new MethodWithParameters();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method2", array, -1);

        Assert.assertArrayEquals(
                new Double[] { firstArg.getNumber(0), firstArg.getNumber(1) },
                component.doubleArg);

        Assert.assertNotNull(component.varArg);
        Assert.assertEquals(0, component.varArg.length);
    }

    @Test
    public void methodWithVarArg_acceptOneValue() {
        JsonArray array = Json.createArray();

        array.set(0, "foo");

        MethodWithVarArgParameter component = new MethodWithVarArgParameter();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "varArgMethod", array, -1);

        Assert.assertEquals(1, component.varArg.length);
        Assert.assertEquals("foo", component.varArg[0]);
    }

    @Test
    public void methodWithVarArg_arrayIsCorrectlyHandled() {
        JsonArray array = Json.createArray();

        JsonArray value = Json.createArray();
        value.set(0, "foo");
        array.set(0, value);

        MethodWithVarArgParameter component = new MethodWithVarArgParameter();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "varArgMethod", array, -1);

        Assert.assertArrayEquals(new String[] { value.getString(0) },
                component.varArg);
    }

    @Test
    public void nullValueAreAcceptedForPrimitive() {
        JsonArray array = Json.createArray();
        array.set(0, Json.createNull());
        MethodWithParameters component = new MethodWithParameters();
        component.intArg = -1;
        component.booleanArg = true;
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "intMethod", array, -1);

        Assert.assertEquals(0, component.intArg);

        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "booleanMethod", array, -1);

        Assert.assertFalse(component.booleanArg);
    }

    @Test(expected = IllegalStateException.class)
    public void noClientCallableMethodException() {
        ComponentWithNoClientCallableMethod component = new ComponentWithNoClientCallableMethod();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "operation", Json.createArray(), -1);
    }

    @Test(expected = IllegalStateException.class)
    public void noMethodException() {
        ComponentWithNoClientCallableMethod component = new ComponentWithNoClientCallableMethod();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "operation1", Json.createArray(), -1);
    }

    @Test
    public void enabledElement_methodIsInvoked() {
        UI ui = new UI();
        ComponentWithCompute component = new ComponentWithCompute();
        ui.add(component);

        requestInvokeMethod(component);

        Assert.assertTrue(component.isInvoked);
    }

    @Test
    public void disabledElement_ClientCallableIsNotInvoked() {
        UI ui = new UI();
        ComponentWithCompute component = new ComponentWithCompute();
        ui.add(component);

        component.getElement().setEnabled(false);

        requestInvokeMethod(component);

        Assert.assertFalse(component.isInvoked);
    }

    @Test
    public void disabledElement_clientDelegateAllowsRPC_methodIsInvoked() {
        UI ui = new UI();
        EnabledHandler component = new EnabledHandler();
        ui.add(component);

        component.getElement().setEnabled(false);

        Assert.assertFalse(component.isInvoked);

        requestInvokeMethod(component, "operation");

        Assert.assertTrue(component.isInvoked);
    }

    private void requestInvokeMethod(Component component) {
        requestInvokeMethod(component, "method");
    }

    private void requestInvokeMethod(Component component, String method) {
        JsonObject json = Json.createObject();
        json.put(JsonConstants.RPC_TEMPLATE_EVENT_METHOD_NAME, method);

        new PublishedServerEventHandlerRpcHandler()
                .handleNode(component.getElement().getNode(), json);
    }

}
