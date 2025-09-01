/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication.rpc;

import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.template.internal.DeprecatedPolymerTemplate;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.JsonConstants;

@NotThreadSafe
public class PublishedServerEventHandlerRpcHandlerTest {

    private VaadinService service;

    private VaadinSession session;

    @Tag("a")
    public static class ComponentWithMethod extends Component
            implements DeprecatedPolymerTemplate {

        private boolean isInvoked;

        protected void intMethod(int i) {
        }

        @EventHandler
        private void method() {
            isInvoked = true;
        }

    }

    @Tag(Tag.DIV)
    public static class EnabledHandler extends Component
            implements DeprecatedPolymerTemplate {

        private boolean isInvoked;

        @EventHandler(DisabledUpdateMode.ALWAYS)
        private void method() {
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
        private boolean booleanArg;
        private String strArg;
        private boolean[] arrayArg;
        private Double[] doubleArg;
        private Integer[] varArg;
        private int[][] doubleArray;
        private JsonNode jsonValue;

        @Override
        @EventHandler
        protected void intMethod(@EventData("foo") int i) {
            intArg = i;
        }

        @EventHandler
        protected void booleanMethod(@EventData("foo") boolean value) {
            booleanArg = value;
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
        protected void method4(@EventData("foo") JsonNode value) {
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
    public void setUp() throws ServiceException {
        Assert.assertNull(System.getSecurityManager());
        service = Mockito.mock(VaadinService.class);

        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.isProductionMode()).thenReturn(false);
        Properties properties = new Properties();
        Mockito.when(configuration.getInitParameters()).thenReturn(properties);

        service = new MockVaadinServletService(configuration);

        VaadinService.setCurrent(service);

        session = Mockito.mock(VaadinSession.class);
        Mockito.when(session.hasLock()).thenReturn(true);
        Mockito.when(session.getService()).thenReturn(service);
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
                component.getClass(), "method", JacksonUtils.createArrayNode(),
                -1);

        Assert.assertTrue(component.isInvoked);
    }

    @Test
    public void methodIsInvokedOnCompositeContent() {
        CompositeOfComponentWithMethod composite = new CompositeOfComponentWithMethod();
        ComponentWithMethod component = composite.getContent();
        PublishedServerEventHandlerRpcHandler.invokeMethod(composite,
                composite.getClass(), "method", JacksonUtils.createArrayNode(),
                -1);

        Assert.assertTrue(component.isInvoked);
    }

    @Test
    public void methodIsInvokectOnCompositeOfComposite() {
        CompositeOfComposite composite = new CompositeOfComposite();
        ComponentWithMethod component = composite.getContent().getContent();
        PublishedServerEventHandlerRpcHandler.invokeMethod(composite,
                composite.getClass(), "method", JacksonUtils.createArrayNode(),
                -1);

        Assert.assertTrue(component.isInvoked);
    }

    @Test(expected = IllegalArgumentException.class)
    public void methodWithoutArgs_argsProvided() {
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(true);
        ComponentWithMethod component = new ComponentWithMethod();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method", args, -1);
    }

    @Test(expected = IllegalStateException.class)
    public void twoEventHandlerMethodsWithTheSameName() {
        ComponentWithTwoEventHandlerMethodSameName component = new ComponentWithTwoEventHandlerMethodSameName();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "intMethod",
                JacksonUtils.createArrayNode(), -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void methodWithParametersInvokedWithoutParameters() {
        MethodWithParameters component = new MethodWithParameters();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "intMethod",
                JacksonUtils.createArrayNode(), -1);
    }

    @Test
    public void methodWithParameterInvokedWithProperParameter() {
        ArrayNode array = JacksonUtils.createArrayNode();
        array.add(65);

        MethodWithParameters component = new MethodWithParameters();

        attachComponent(component);

        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "intMethod", array, -1);

        Assert.assertEquals(65, component.intArg);
    }

    @Test
    public void methodWithArrayParamIsInvoked() {
        ArrayNode array = JacksonUtils.createArrayNode();
        array.add("foo");
        ArrayNode secondArg = JacksonUtils.createArrayNode();
        secondArg.add(true);
        secondArg.add(false);
        array.add(secondArg);
        MethodWithParameters component = new MethodWithParameters();
        attachComponent(component);
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method1", array, -1);

        Assert.assertEquals("foo", component.strArg);
        Assert.assertArrayEquals(new boolean[] { true, false },
                component.arrayArg);
    }

    @Test
    public void methodWithVarArgIsInvoked_varArgsAreNotArray() {
        ArrayNode array = JacksonUtils.createArrayNode();

        ArrayNode firstArg = JacksonUtils.createArrayNode();
        firstArg.add(3.1d);
        firstArg.add(65.57d);

        array.add(firstArg);

        array.add(JacksonUtils.nullNode());
        array.add(56);

        MethodWithParameters component = new MethodWithParameters();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method2", array, -1);

        Assert.assertArrayEquals(new Double[] { firstArg.get(0).doubleValue(),
                firstArg.get(1).doubleValue() }, component.doubleArg);

        Assert.assertNotNull(component.varArg);
        Assert.assertNull(component.varArg[0]);
        Assert.assertEquals(Integer.valueOf(56), component.varArg[1]);
        Assert.assertEquals(2, component.varArg.length);
    }

    @Test
    public void methodWithDoubleArrayIsInvoked() {
        ArrayNode array = JacksonUtils.createArrayNode();

        ArrayNode arg = JacksonUtils.createArrayNode();

        ArrayNode first = JacksonUtils.createArrayNode();
        first.add(1);
        first.add(2);

        arg.add(first);

        ArrayNode second = JacksonUtils.createArrayNode();
        second.add(3);
        second.add(4);

        arg.add(second);

        array.add(arg);

        MethodWithParameters component = new MethodWithParameters();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method3", array, -1);

        Assert.assertArrayEquals(
                new int[] { first.get(0).intValue(), first.get(1).intValue() },
                component.doubleArray[0]);

        Assert.assertArrayEquals(new int[] { second.get(0).intValue(),
                second.get(1).intValue() }, component.doubleArray[1]);
    }

    @Test
    public void methodWithJsonValueIsInvoked() {
        ArrayNode array = JacksonUtils.createArrayNode();

        ObjectNode json = JacksonUtils.createObjectNode();
        json.put("foo", "bar");
        array.add(json);

        MethodWithParameters component = new MethodWithParameters();
        attachComponent(component);
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method4", array, -1);

        Assert.assertEquals(component.jsonValue, json);
    }

    @Test
    public void methodWithVarArgIsInvoked_varArgsIsArray() {
        ArrayNode array = JacksonUtils.createArrayNode();

        ArrayNode firstArg = JacksonUtils.createArrayNode();
        firstArg.add(5.6d);
        firstArg.add(78.36d);

        array.add(firstArg);

        ArrayNode secondArg = JacksonUtils.createArrayNode();
        secondArg.add(5);
        secondArg.add(JacksonUtils.nullNode());
        secondArg.add(2);
        array.add(secondArg);

        MethodWithParameters component = new MethodWithParameters();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "method2", array, -1);

        Assert.assertArrayEquals(new Double[] { firstArg.get(0).doubleValue(),
                firstArg.get(1).doubleValue() }, component.doubleArg);

        Assert.assertNotNull(component.varArg);
        Assert.assertArrayEquals(new Integer[] { secondArg.get(0).intValue(),
                null, secondArg.get(2).intValue() }, component.varArg);
    }

    @Test
    public void methodWithVarArg_acceptNoValues() {
        ArrayNode array = JacksonUtils.createArrayNode();

        MethodWithVarArgParameter component = new MethodWithVarArgParameter();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "varArgMethod", array, -1);

        Assert.assertEquals(0, component.varArg.length);
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

        Assert.assertArrayEquals(new Double[] { firstArg.get(0).doubleValue(),
                firstArg.get(1).doubleValue() }, component.doubleArg);

        Assert.assertNotNull(component.varArg);
        Assert.assertEquals(0, component.varArg.length);
    }

    @Test
    public void methodWithVarArg_acceptOneValue() {
        ArrayNode array = JacksonUtils.createArrayNode();

        array.add("foo");

        MethodWithVarArgParameter component = new MethodWithVarArgParameter();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "varArgMethod", array, -1);

        Assert.assertEquals(1, component.varArg.length);
        Assert.assertEquals("foo", component.varArg[0]);
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

        Assert.assertArrayEquals(new String[] { value.get(0).asText() },
                component.varArg);
    }

    @Test
    public void nullValueAreAcceptedForPrimitive() {
        ArrayNode array = JacksonUtils.createArrayNode();
        array.add(JacksonUtils.nullNode());
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
    public void noEventHandlerMethodException() {
        ComponentWithNoEventHandlerMethod component = new ComponentWithNoEventHandlerMethod();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "operation",
                JacksonUtils.createArrayNode(), -1);
    }

    @Test(expected = IllegalStateException.class)
    public void noMethodException() {
        ComponentWithNoEventHandlerMethod component = new ComponentWithNoEventHandlerMethod();
        PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                component.getClass(), "operation1",
                JacksonUtils.createArrayNode(), -1);
    }

    @Test
    public void methodThrowsException_exceptionHasCorrectCause() {
        ComponentWithMethodThrowingException component = new ComponentWithMethodThrowingException();
        try {
            PublishedServerEventHandlerRpcHandler.invokeMethod(component,
                    component.getClass(), "method",
                    JacksonUtils.createArrayNode(), -1);
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getCause() instanceof NullPointerException);
        }
    }

    @Test
    public void enabledElement_methodIsInvoked() {
        ComponentWithMethod component = new ComponentWithMethod();
        attachComponent(component);

        requestInvokeMethod(component);

        Assert.assertTrue(component.isInvoked);
    }

    @Test
    public void disabledElement_eventHandlerIsNotInvoked() {
        ComponentWithMethod component = new ComponentWithMethod();
        attachComponent(component);

        component.getElement().setEnabled(false);

        requestInvokeMethod(component);

        Assert.assertFalse(component.isInvoked);
    }

    @Test
    public void implicitelyDisabledElement_eventHandlerIsNotInvoked() {
        ComponentWithMethod component = new ComponentWithMethod();

        attachComponent(component).setEnabled(false);

        requestInvokeMethod(component);

        Assert.assertFalse(component.isInvoked);
    }

    @Test
    public void disabledElement_eventHandlerAllowsRPC_methodIsInvoked() {
        EnabledHandler component = new EnabledHandler();
        attachComponent(component);

        component.getElement().setEnabled(false);

        Assert.assertFalse(component.isInvoked);

        requestInvokeMethod(component);

        Assert.assertTrue(component.isInvoked);
    }

    private UI attachComponent(Component component) {
        UI ui = new UI();
        ui.getInternals().setSession(session);

        ui.add(component);
        return ui;
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
