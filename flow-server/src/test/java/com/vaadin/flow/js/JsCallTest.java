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
package com.vaadin.flow.js;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsCallTest {

    @JsDefinition
    interface GreeterJs extends Serializable {
        @JsExpression("window.alert($0)")
        void showGreeting(String greeting);

        void undeclared();

        @JsExpression("this.ambiguous($0)")
        void ambiguous(String value);

        @JsExpression("this.ambiguous($0)")
        void ambiguous(int value);
    }

    private static class Greeter implements GreeterJs {
        private final List<String> greetings = new java.util.ArrayList<>();

        @Override
        public void showGreeting(String greeting) {
            greetings.add(greeting);
        }

        @Override
        public void undeclared() {
            throw new UnsupportedOperationException("nothing to do here");
        }

        @Override
        public void ambiguous(String value) {
        }

        @Override
        public void ambiguous(int value) {
        }
    }

    private static JsCall call(String methodName, Object... arguments) {
        return new JsCall(GreeterJs.class, methodName,
                Arrays.asList(arguments));
    }

    @Test
    void getExpression_methodWithoutDeclaredJavaScript_throws() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> call("undeclared").getExpression());

        assertTrue(exception.getMessage().contains("@JsExpression"),
                "the message should name what the method is missing: "
                        + exception.getMessage());
    }

    @Test
    void getExpression_overloadsWithTheSameArity_throws() {
        // Overloads are resolved by name and argument count, so two of them
        // with the same arity can not be told apart
        assertThrows(IllegalStateException.class,
                () -> call("ambiguous", "value").getExpression());
    }

    @Test
    void invokeOn_implementation_runsTheMethod() {
        Greeter greeter = new Greeter();

        call("showGreeting", "Hello").invokeOn(greeter);

        assertEquals(List.of("Hello"), greeter.greetings);
    }

    @Test
    void nullArgument_keptAndPassedToTheImplementation() {
        // What a method whose value is optional is called with - the focus
        // options of a browser, for one - so it has to survive the call and
        // reach the implementation as it was.
        JsCall call = call("showGreeting", (Object) null);
        Greeter greeter = new Greeter();

        assertEquals(Collections.singletonList(null), call.arguments());

        call.invokeOn(greeter);

        assertEquals(Collections.singletonList(null), greeter.greetings);
    }

    @Test
    void invokeOn_somethingElse_throws() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> call("showGreeting", "Hello").invokeOn("not a greeter"));

        assertTrue(exception.getMessage().contains(GreeterJs.class.getName()),
                "the message should name the interface that was expected: "
                        + exception.getMessage());
    }

    @Test
    void invokeOn_implementationThrows_theFailureReachesTheCaller() {
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> call("undeclared").invokeOn(new Greeter()));

        assertEquals("nothing to do here", exception.getMessage(),
                "what the implementation threw should not be wrapped");
    }
}
