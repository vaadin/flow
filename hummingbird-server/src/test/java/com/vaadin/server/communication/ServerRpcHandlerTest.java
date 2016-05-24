/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.server.communication;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.Tag;
import com.vaadin.ui.Component;

import elemental.json.Json;
import elemental.json.JsonArray;

/**
 * @author Vaadin Ltd
 *
 */
public class ServerRpcHandlerTest {

    @Tag("a")
    public static class ComponentWithMethod extends Component {

        private boolean isInvoked;

        protected void method(int i) {

        }

        @EventHandler
        private void method() {
            isInvoked = true;
        }
    }

    public static class ComponentWithTwoEventHandlerMethodSameName
            extends ComponentWithMethod {

        @EventHandler
        @Override
        protected void method(int i) {

        }

        @EventHandler
        private void method() {
        }
    }

    public static class MethodWithParameters extends ComponentWithMethod {

        private int intArg;
        private String strArg;
        private boolean[] arrayArg;
        private Double[] doubleArg;
        private Integer[] varArg;
        private int[][] doubleArray;

        @Override
        @EventHandler
        protected void method(int i) {
            intArg = i;
        }

        @EventHandler
        protected void method1(String str, boolean[] array) {
            strArg = str;
            arrayArg = array;
        }

        @EventHandler
        protected void method2(Double[] arg1, Integer... varArg) {
            doubleArg = arg1;
            this.varArg = varArg;
        }

        @EventHandler
        protected void method3(int[][] array) {
            doubleArray = array;
        }
    }

    public static class MethodWithVarArgParameter extends ComponentWithMethod {

        private String[] varArg;

        @EventHandler
        protected void method(String... args) {
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

    @Before
    public void setUp() {
        Assert.assertNull(System.getSecurityManager());
    }

    @Test
    public void methodIsInvoked() {
        ComponentWithMethod component = new ComponentWithMethod();
        ServerRpcHandler.invokeMethod(component, component.getClass(), "method",
                Json.createArray());

        Assert.assertTrue(component.isInvoked);
    }

    @Test(expected = IllegalArgumentException.class)
    public void methodWithoutArgs_argsProvided() {
        JsonArray args = Json.createArray();
        args.set(0, true);
        ComponentWithMethod component = new ComponentWithMethod();
        ServerRpcHandler.invokeMethod(component, component.getClass(), "method",
                args);
    }

    @Test(expected = IllegalStateException.class)
    public void twoEventHandlerMethodsWithTheSameName() {
        ComponentWithMethod component = new ComponentWithTwoEventHandlerMethodSameName();
        ServerRpcHandler.invokeMethod(component, component.getClass(), "method",
                Json.createArray());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parameterizedMethodIsNotAccepted() {
        ComponentWithMethod component = new MethodWithParameters();
        ServerRpcHandler.invokeMethod(component, component.getClass(), "method",
                Json.createArray());
    }

    @Test
    public void parameterizedMethodIsInvoked() {
        JsonArray array = Json.createArray();
        array.set(0, 65);
        MethodWithParameters component = new MethodWithParameters();
        ServerRpcHandler.invokeMethod(component, component.getClass(), "method",
                array);

        Assert.assertEquals(65, component.intArg);
    }

    @Test
    public void parameterizedMethodWithArrayParamIsInvoked() {
        JsonArray array = Json.createArray();
        array.set(0, "foo");
        JsonArray secondArg = Json.createArray();
        secondArg.set(0, true);
        secondArg.set(1, false);
        array.set(1, secondArg);
        MethodWithParameters component = new MethodWithParameters();
        ServerRpcHandler.invokeMethod(component, component.getClass(),
                "method1", array);

        Assert.assertEquals("foo", component.strArg);
        Assert.assertArrayEquals(new boolean[] { true, false },
                component.arrayArg);
    }

    @Test
    public void parameterizedMethodWithVarArgIsInvoked_varArgsAreNotArray() {
        JsonArray array = Json.createArray();

        JsonArray firstArg = Json.createArray();
        firstArg.set(0, 3.1d);
        firstArg.set(1, 65.57d);

        array.set(0, firstArg);

        array.set(1, Json.createNull());
        array.set(2, 56);

        MethodWithParameters component = new MethodWithParameters();
        ServerRpcHandler.invokeMethod(component, component.getClass(),
                "method2", array);

        Assert.assertArrayEquals(
                new Double[] { firstArg.getNumber(0), firstArg.getNumber(1) },
                component.doubleArg);

        Assert.assertNotNull(component.varArg);
        Assert.assertNull(component.varArg[0]);
        Assert.assertEquals(Integer.valueOf(56), component.varArg[1]);
        Assert.assertEquals(2, component.varArg.length);
    }

    @Test
    public void parameterizedMethodWithDoubleArrayIsInvoked() {
        JsonArray array = Json.createArray();

        JsonArray firstArg = Json.createArray();
        firstArg.set(0, 1);
        firstArg.set(1, 2);

        array.set(0, firstArg);

        JsonArray secondArg = Json.createArray();
        secondArg.set(0, 3);
        secondArg.set(1, 4);

        array.set(1, secondArg);

        MethodWithParameters component = new MethodWithParameters();
        ServerRpcHandler.invokeMethod(component, component.getClass(),
                "method3", array);

        Assert.assertArrayEquals(
                new int[] { (int) firstArg.getNumber(0),
                        (int) firstArg.getNumber(1) },
                component.doubleArray[0]);

        Assert.assertArrayEquals(
                new int[] { (int) secondArg.getNumber(0),
                        (int) secondArg.getNumber(1) },
                component.doubleArray[1]);

    }

    @Test
    public void parameterizedMethodWithVarArgIsInvoked_varArgsIsArray() {
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
        ServerRpcHandler.invokeMethod(component, component.getClass(),
                "method2", array);

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
        ServerRpcHandler.invokeMethod(component, component.getClass(), "method",
                array);

        Assert.assertEquals(0, component.varArg.length);
    }

    @Test
    public void methodWithVarArg_acceptOneValue() {
        JsonArray array = Json.createArray();

        array.set(0, "foo");

        MethodWithVarArgParameter component = new MethodWithVarArgParameter();
        ServerRpcHandler.invokeMethod(component, component.getClass(), "method",
                array);

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
        ServerRpcHandler.invokeMethod(component, component.getClass(), "method",
                array);

        Assert.assertArrayEquals(new String[] { value.getString(0) },
                component.varArg);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullValueIsNotAcceptedForPrimitive() {
        JsonArray array = Json.createArray();
        array.set(0, Json.createNull());
        MethodWithParameters component = new MethodWithParameters();
        ServerRpcHandler.invokeMethod(component, component.getClass(), "method",
                array);
    }

    @Test(expected = IllegalStateException.class)
    public void noEventHandlerMethodException() {
        ComponentWithNoEventHandlerMethod component = new ComponentWithNoEventHandlerMethod();
        ServerRpcHandler.invokeMethod(component, component.getClass(),
                "operation", Json.createArray());
    }

    @Test(expected = IllegalStateException.class)
    public void noMethodException() {
        ComponentWithNoEventHandlerMethod component = new ComponentWithNoEventHandlerMethod();
        ServerRpcHandler.invokeMethod(component, component.getClass(),
                "operation1", Json.createArray());
    }

    @Test
    public void methodThrowsException_exceptionHasCorrectCause() {
        ComponentWithMethodThrowingException component = new ComponentWithMethodThrowingException();
        try {
            ServerRpcHandler.invokeMethod(component, component.getClass(),
                    "method", Json.createArray());
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getCause() instanceof NullPointerException);
        }
    }

}
