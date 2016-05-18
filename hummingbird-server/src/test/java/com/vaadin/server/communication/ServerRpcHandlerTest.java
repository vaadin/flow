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

    public static class ComponentWithParameterizedMethod
            extends ComponentWithMethod {

        @Override
        @EventHandler
        protected void method(int i) {
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
        ServerRpcHandler.invokeMethod(component, component.getClass(),
                "method");

        Assert.assertTrue(component.isInvoked);
    }

    @Test
    public void parentMethodIsInvoked() {
        ComponentWithMethod component = new ComponentWithParameterizedMethod();
        ServerRpcHandler.invokeMethod(component, component.getClass(),
                "method");

        Assert.assertTrue(component.isInvoked);
    }

    @Test(expected = IllegalStateException.class)
    public void noEventHandlerMethodException() {
        ComponentWithNoEventHandlerMethod component = new ComponentWithNoEventHandlerMethod();
        ServerRpcHandler.invokeMethod(component, component.getClass(),
                "operation");
    }

    @Test(expected = IllegalStateException.class)
    public void noMethodException() {
        ComponentWithNoEventHandlerMethod component = new ComponentWithNoEventHandlerMethod();
        ServerRpcHandler.invokeMethod(component, component.getClass(),
                "operation1");
    }

    @Test
    public void methodThrowsException_exceptionHasCorrectCause() {
        ComponentWithMethodThrowingException component = new ComponentWithMethodThrowingException();
        try {
            ServerRpcHandler.invokeMethod(component, component.getClass(),
                    "method");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getCause() instanceof NullPointerException);
        }
    }

}
