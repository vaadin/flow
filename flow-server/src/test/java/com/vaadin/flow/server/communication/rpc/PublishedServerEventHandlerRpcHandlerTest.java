/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.polymertemplate.TemplateParser.TemplateData;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.tests.util.MockUI;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

@NotThreadSafe
public class PublishedServerEventHandlerRpcHandlerTest {

    private VaadinService service;

    @Tag("a")
    public static class ComponentWithMethod
            extends PolymerTemplate<TemplateModel> {

        private boolean isInvoked;

        public ComponentWithMethod() {
            super((clazz, tag, service) -> new TemplateData("",
                    new Element("a")));
        }

        protected void intMethod(int i) {
        }

        @EventHandler
        private void method() {
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
    public static class EnabledHandler extends PolymerTemplate<TemplateModel> {

        private boolean isInvoked;

        EnabledHandler() {
            super((clazz, tag, service) -> new TemplateData("",
                    new Element("div")));
        }

        @EventHandler(DisabledUpdateMode.ALWAYS)
        private void method() {
            isInvoked = true;
        }

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

    public static class ComponentWithTwoEventHandlerMethodSameName
            extends ComponentWithMethod {

        @EventHandler
        @Override
        protected void intMethod(int i) {

        }

        @EventHandler
        private void intMethod() {
        }
    }

    public static class MethodWithParameters extends ComponentWithMethod {

        private int intArg;
        private String strArg;
        private boolean[] arrayArg;
        private Double[] doubleArg;
        private Integer[] varArg;
        private int[][] doubleArray;
        private JsonValue jsonValue;

        @Override
        @EventHandler
        protected void intMethod(@EventData("foo") int i) {
            intArg = i;
        }

        @EventHandler
        protected void method1(@EventData("foo") String str,
                @EventData("bar") boolean[] array) {
            strArg = str;
            arrayArg = array;
        }

        @EventHandler
        protected void method2(@EventData("foo") Double[] arg1,
                @EventData("bar") Integer... varArg) {
            doubleArg = arg1;
            this.varArg = varArg;
        }

        @EventHandler
        protected void method3(@EventData("foo") int[][] array) {
            doubleArray = array;
        }

        @EventHandler
        protected void method4(@EventData("foo") JsonValue value) {
            jsonValue = value;
        }
    }

    public static class MethodWithVarArgParameter extends ComponentWithMethod {

        private String[] varArg;

        @EventHandler
        protected void varArgMethod(@EventData("foo") String... args) {
            varArg = args;
        }

    }

    public static class ComponentWithMethodThrowingException
            extends ComponentWithMethod {

        @EventHandler
        private void method() {
            throw new NullPointerException();
        }
    }

    public static class ComponentWithNoEventHandlerMethod
            extends ComponentWithMethod {

        /**
         * No {@link EventHandler} annotation.
         */
        public void operation() {
        }
    }

    public static class CompositeOfComponentWithMethod
            extends Composite<ComponentWithMethod> {
    }

    public static class CompositeOfComposite
            extends Composite<CompositeOfComponentWithMethod> {
    }

    @Before
    public void setUp() {
        Assert.assertNull(System.getSecurityManager());
        service = Mockito.mock(VaadinService.class);

        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.isProductionMode()).thenReturn(false);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        VaadinService.setCurrent(service);
    }

    @After
    public void tearDown() {
        service = null;
        VaadinService.setCurrent(null);
    }

    @Test
    public void methodIsInvoked() {
        ComponentWithMethod component = new ComponentWithMethod();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method", Json.createArray(), -1);

        Assert.assertTrue(component.isInvoked);
    }

    @Test
    public void methodIsInvokedOnCompositeContent() {
        CompositeOfComponentWithMethod composite = new CompositeOfComponentWithMethod();
        ComponentWithMethod component = composite.getContent();
        PublishedServerEventHandlerRpcHandler.invokeMethod(composite,
                composite.getClass(), "method", Json.createArray(), -1);

        Assert.assertTrue(component.isInvoked);
    }

    @Test
    public void methodIsInvokectOnCompositeOfComposite() {
        CompositeOfComposite composite = new CompositeOfComposite();
        ComponentWithMethod component = composite.getContent().getContent();
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
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method", params, -1);

        Assert.assertTrue(component.isInvoked);
    }

    @Test(expected = IllegalArgumentException.class)
    public void methodWithDecoderParameters_nonConvertableValues_methodIsInvoked() {
        JsonArray params = Json.createArray();
        params.set(0, "264.1");
        params.set(1, "MR");

        DecoderParameters component = new DecoderParameters();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method", params, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void methodWithoutArgs_argsProvided() {
        JsonArray args = Json.createArray();
        args.set(0, true);
        ComponentWithMethod component = new ComponentWithMethod();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method", args, -1);
    }

    @Test
    public void promiseSuccess() {
        int promiseId = 4;

        JsonArray args = Json.createArray();
        args.set(0, 36);

        ComponentWithMethod component = new ComponentWithMethod();
        MockUI ui = new MockUI();
        ui.add(component);

        // Get rid of attach invocations
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().dumpPendingJavaScriptInvocations();

        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "compute", args, promiseId);

        List<PendingJavaScriptInvocation> pendingJavaScriptInvocations = ui
                .dumpPendingJsInvocations();
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

        ComponentWithMethod component = new ComponentWithMethod();
        MockUI ui = new MockUI();
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

        List<PendingJavaScriptInvocation> pendingJavaScriptInvocations = ui
                .dumpPendingJsInvocations();
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

    @Test(expected = IllegalStateException.class)
    public void twoEventHandlerMethodsWithTheSameName() {
        ComponentWithTwoEventHandlerMethodSameName component = new ComponentWithTwoEventHandlerMethodSameName();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "intMethod", Json.createArray(), -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void methodWithParametersInvokedWithoutParameters() {
        MethodWithParameters component = new MethodWithParameters();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "intMethod", Json.createArray(), -1);
    }

    @Test
    public void methodWithParameterInvokedWithProperParameter() {
        JsonArray array = Json.createArray();
        array.set(0, 65);
        MethodWithParameters component = new MethodWithParameters();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "intMethod", array, -1);

        Assert.assertEquals(65, component.intArg);
    }

    @Test
    public void methodWithArrayParamIsInvoked() {
        JsonArray array = Json.createArray();
        array.set(0, "foo");
        JsonArray secondArg = Json.createArray();
        secondArg.set(0, true);
        secondArg.set(1, false);
        array.set(1, secondArg);
        MethodWithParameters component = new MethodWithParameters();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method1", array, -1);

        Assert.assertEquals("foo", component.strArg);
        Assert.assertArrayEquals(new boolean[] { true, false },
                component.arrayArg);
    }

    @Test
    public void methodWithVarArgIsInvoked_varArgsAreNotArray() {
        JsonArray array = Json.createArray();

        JsonArray firstArg = Json.createArray();
        firstArg.set(0, 3.1d);
        firstArg.set(1, 65.57d);

        array.set(0, firstArg);

        array.set(1, Json.createNull());
        array.set(2, 56);

        MethodWithParameters component = new MethodWithParameters();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method2", array, -1);

        Assert.assertArrayEquals(
                new Double[] { firstArg.getNumber(0), firstArg.getNumber(1) },
                component.doubleArg);

        Assert.assertNotNull(component.varArg);
        Assert.assertNull(component.varArg[0]);
        Assert.assertEquals(Integer.valueOf(56), component.varArg[1]);
        Assert.assertEquals(2, component.varArg.length);
    }

    @Test
    public void methodWithDoubleArrayIsInvoked() {
        JsonArray array = Json.createArray();

        JsonArray arg = Json.createArray();

        JsonArray first = Json.createArray();
        first.set(0, 1);
        first.set(1, 2);

        arg.set(0, first);

        JsonArray second = Json.createArray();
        second.set(0, 3);
        second.set(1, 4);

        arg.set(1, second);

        array.set(0, arg);

        MethodWithParameters component = new MethodWithParameters();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method3", array, -1);

        Assert.assertArrayEquals(new int[] { (int) first.getNumber(0),
                (int) first.getNumber(1) }, component.doubleArray[0]);

        Assert.assertArrayEquals(new int[] { (int) second.getNumber(0),
                (int) second.getNumber(1) }, component.doubleArray[1]);
    }

    @Test
    public void methodWithJsonValueIsInvoked() {
        JsonArray array = Json.createArray();

        JsonObject json = Json.createObject();
        json.put("foo", "bar");
        array.set(0, json);

        MethodWithParameters component = new MethodWithParameters();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method4", array, -1);

        Assert.assertEquals(component.jsonValue, json);
    }

    @Test
    public void methodWithVarArgIsInvoked_varArgsIsArray() {
        JsonArray array = Json.createArray();

        JsonArray firstArg = Json.createArray();
        firstArg.set(0, 5.6d);
        firstArg.set(1, 78.36d);

        array.set(0, firstArg);

        JsonArray secondArg = Json.createArray();
        secondArg.set(0, 5);
        secondArg.set(1, Json.createNull());
        secondArg.set(2, 2);
        array.set(1, secondArg);

        MethodWithParameters component = new MethodWithParameters();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method2", array, -1);

        Assert.assertArrayEquals(
                new Double[] { firstArg.getNumber(0), firstArg.getNumber(1) },
                component.doubleArg);

        Assert.assertNotNull(component.varArg);
        Assert.assertArrayEquals(new Integer[] { (int) secondArg.getNumber(0),
                null, (int) secondArg.getNumber(2) }, component.varArg);
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

    @Test(expected = IllegalArgumentException.class)
    public void nullValueIsNotAcceptedForPrimitive() {
        JsonArray array = Json.createArray();
        array.set(0, Json.createNull());
        MethodWithParameters component = new MethodWithParameters();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method", array, -1);
    }

    @Test(expected = IllegalStateException.class)
    public void noEventHandlerMethodException() {
        ComponentWithNoEventHandlerMethod component = new ComponentWithNoEventHandlerMethod();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "operation", Json.createArray(), -1);
    }

    @Test(expected = IllegalStateException.class)
    public void noMethodException() {
        ComponentWithNoEventHandlerMethod component = new ComponentWithNoEventHandlerMethod();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "operation1", Json.createArray(), -1);
    }

    @Test
    public void methodThrowsException_exceptionHasCorrectCause() {
        ComponentWithMethodThrowingException component = new ComponentWithMethodThrowingException();
        try {
            PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                    component.getClass(), "method", Json.createArray(), -1);
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getCause() instanceof NullPointerException);
        }
    }

    @Test
    public void enabledElement_methodIsInvoked() {
        UI ui = new UI();
        ComponentWithMethod component = new ComponentWithMethod();
        ui.add(component);

        requestInvokeMethod(component);

        Assert.assertTrue(component.isInvoked);
    }

    @Test
    public void disabledElement_eventHandlerIsNotInvoked() {
        UI ui = new UI();
        ComponentWithMethod component = new ComponentWithMethod();
        ui.add(component);

        component.getElement().setEnabled(false);

        requestInvokeMethod(component);

        Assert.assertFalse(component.isInvoked);
    }

    @Test
    public void implicitelyDisabledElement_eventHandlerIsNotInvoked() {
        UI ui = new UI();
        ComponentWithMethod component = new ComponentWithMethod();
        ui.add(component);

        ui.setEnabled(false);

        requestInvokeMethod(component);

        Assert.assertFalse(component.isInvoked);
    }

    @Test
    public void disabledElement_eventHandlerAllowsRPC_methodIsInvoked() {
        UI ui = new UI();
        EnabledHandler component = new EnabledHandler();
        ui.add(component);

        component.getElement().setEnabled(false);

        Assert.assertFalse(component.isInvoked);

        requestInvokeMethod(component);

        Assert.assertTrue(component.isInvoked);
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
