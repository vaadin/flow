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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.dom.JsFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

        @JsExpression("this.shout($0, ...$1)")
        void shout(String greeting, Object... names);

        @JsExpression("this.count(...$0)")
        void count(int... values);
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

        @Override
        public void shout(String greeting, Object... names) {
            greetings.add(greeting + " " + Arrays.toString(names));
        }

        @Override
        public void count(int... values) {
            greetings.add(Arrays.toString(values));
        }
    }

    interface LoggerJs extends Serializable {
        @JsExpression("console.log($0, value)")
        void log(String prefix);

        @JsExpression("console.log($0, value)")
        JsFunction createWithoutParameters(String prefix);

        @JsExpression("console.log($0, value)")
        @JsCalledWith("value")
        JsFunction createLogger(String prefix);

        @JsExpression("console.log($0, value)")
        @JsCalledWith("other")
        JsFunction createOtherLogger(String prefix);
    }

    interface InvalidNamesJs extends Serializable {
        @JsExpression("value")
        @JsCalledWith("not-a-name")
        JsFunction notAnIdentifier();

        @JsExpression("value")
        @JsCalledWith("class")
        JsFunction reservedWord();

        @JsExpression("value")
        @JsCalledWith("$0")
        JsFunction captureName(String value);

        @JsExpression("value")
        @JsCalledWith({ "value", "value" })
        JsFunction duplicate();
    }

    private static JsCall call(String methodName, Object... arguments) {
        return new JsCall(GreeterJs.class, methodName,
                Arrays.asList(arguments));
    }

    @Test
    void parametersFor_argumentsFollowedByWhatToRunOn() {
        // The element a call was made on goes in the last place, and a call
        // made on nothing in particular leaves it empty, so a client reads
        // the two the same way
        Element element = ElementFactory.createDiv();

        assertEquals(Arrays.asList("Hello", element), Arrays
                .asList(call("showGreeting", "Hello").parametersFor(element)));
        assertEquals(Arrays.asList("Hello", null), Arrays
                .asList(call("showGreeting", "Hello").parametersFor(null)));
    }

    @Test
    void getExpression_methodWithoutDeclaredJavaScript_throws() {
        JsCall undeclared = call("undeclared");
        IllegalStateException exception = assertThrows(
                IllegalStateException.class, undeclared::getExpression);

        assertTrue(exception.getMessage().contains("@JsExpression"),
                "the message should name what the method is missing: "
                        + exception.getMessage());
        assertThrows(IllegalStateException.class, undeclared::getFunctionId);
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

    @Test
    void variadicCall_argumentsFlattenedButKeptPackedForJava() {
        JsCall call = call("shout", "Hello", new Object[] { "Alice", "Bob" });
        Greeter greeter = new Greeter();

        assertTrue(call.isVariadic());
        assertEquals(List.of("Hello", "Alice", "Bob"), call.flattenArguments(),
                "the client should get one value per parameter of the generated function");

        call.invokeOn(greeter);

        assertEquals(List.of("Hello [Alice, Bob]"), greeter.greetings,
                "while Java should get the trailing arguments packed, as it declared them");
    }

    @Test
    void variadicCall_primitiveTail_flattensToBoxedValues() {
        // Nothing says a declaration has to collect its arguments as objects,
        // and the client is sent one encodable value per argument either way
        JsCall call = call("count", new int[] { 1, 2, 3 });
        Greeter greeter = new Greeter();

        assertEquals(List.of(1, 2, 3), call.flattenArguments());

        call.invokeOn(greeter);

        assertEquals(List.of("[1, 2, 3]"), greeter.greetings);
    }

    @Test
    void variadicCall_noTrailingArguments_flattensToTheFixedOnes() {
        assertEquals(List.of("Hello"),
                call("shout", "Hello", new Object[0]).flattenArguments());
        assertEquals(List.of("Hello"),
                call("shout", "Hello", (Object[]) null).flattenArguments(),
                "a null array should read as no trailing arguments, the way Java reads it");
    }

    @Test
    void functionId_tellsAVariadicDeclarationFromAFixedOne() {
        // Two methods can declare the same JavaScript for the same number of
        // parameters and still be two functions, since the generated one
        // collects the trailing arguments in only one of them
        assertNotEquals(JsCall.functionId("this.run($0)", 1, true),
                JsCall.functionId("this.run($0)", 1, false));
    }

    @Test
    void functionId_snippetGeneratedDifferentlyFromTheSameJavaScript_isAnotherFunction() {
        // A call runs an async function, a snippet a synchronous one, which
        // wraps the expression in a function taking the declared parameters:
        // each of them is generated differently, so none
        // of them may be registered under the identifier of another
        Set<String> ids = Stream.of(LoggerJs.class.getMethods())
                .map(JsCall::functionId).collect(Collectors.toSet());

        assertEquals(LoggerJs.class.getMethods().length, ids.size(),
                "every way of generating the same JavaScript should have an identifier of its own");
    }

    @Test
    void getCalledWithNames_answersWhatTheMethodDeclares() throws Exception {
        assertEquals(List.of("value"), JsCall.getCalledWithNames(
                LoggerJs.class.getMethod("createLogger", String.class)));
        assertEquals(List.of(),
                JsCall.getCalledWithNames(LoggerJs.class
                        .getMethod("createWithoutParameters", String.class)),
                "a method without the annotation declares a function taking no parameters");
        // A call reads them off the method it calls, and only a snippet has
        // a function around its expression to name parameters of
        assertEquals(List.of("value"),
                new JsCall(LoggerJs.class, "createLogger", List.of("a"))
                        .getCalledWithNames());
        assertEquals(List.of(), new JsCall(LoggerJs.class,
                "createWithoutParameters", List.of("a")).getCalledWithNames());
        assertEquals(List.of(), new JsCall(LoggerJs.class, "log", List.of("a"))
                .getCalledWithNames());
    }

    @ParameterizedTest
    @ValueSource(strings = { "notAnIdentifier", "reservedWord", "captureName",
            "duplicate" })
    void getCalledWithNames_nameAFunctionCanNotDeclare_throws(
            String methodName) {
        // The names go into the generated module, where one of these is a
        // syntax error that would break every function in it
        Method method = Stream.of(InvalidNamesJs.class.getMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst().orElseThrow();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsCall.getCalledWithNames(method));
        assertTrue(exception.getMessage().contains(methodName),
                "the message should name the method: "
                        + exception.getMessage());
    }
}
