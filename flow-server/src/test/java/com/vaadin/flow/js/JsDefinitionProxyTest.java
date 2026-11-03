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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.DomEventData;
import com.vaadin.flow.dom.DomEventFilter;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class JsDefinitionProxyTest {

    @JsDefinition
    interface GreeterJs extends Serializable {
        @JsExpression("window.alert($0)")
        void showGreeting(String greeting);

        @JsExpression("return this.value;")
        PendingJavaScriptResult readValue();
    }

    @JsDefinition
    interface UndeclaredJs extends Serializable {
        void undeclared();
    }

    @JsDefinition
    interface UnsupportedJs extends Serializable {
        @JsExpression("return this.value;")
        String readValue();
    }

    @JsDefinition
    interface ComposingJs extends Serializable {
        @JsExpression("this.method()")
        void method();

        default void twice() {
            method();
            method();
        }
    }

    @JsDefinition
    interface HelpingJs extends Serializable {
        @JsExpression("this.method()")
        void method();

        static String name() {
            return "HelpingJs";
        }
    }

    @JsDefinition
    interface LoggerJs extends Serializable {
        @JsExpression("console.log($0, value);")
        @JsCalledWith("value")
        JsFunction createLogger(String prefix);

        @JsExpression("return (value) => console.log($0, value);")
        JsSnippet createSnippet(String prefix);
    }

    @JsDefinition
    interface EventJs extends Serializable {
        @JsExpression("return event.key === $0;")
        DomEventFilter isKey(String key);

        @JsExpression("return element.getAttribute($0);")
        DomEventData<String> attribute(String name);

        @JsExpression("return event.detail === $0;")
        DomEventFilter isDetail(Object detail);

        @JsExpression("return $0;")
        DomEventData<Object> echo(Object value);
    }

    /**
     * A bean that cannot be encoded since it refers to itself.
     */
    public static class SelfReference {
        public SelfReference getSelf() {
            return this;
        }
    }

    @JsDefinition
    interface CalledWithInvalidNameJs extends Serializable {
        @JsExpression("console.log(value);")
        @JsCalledWith("not-a-name")
        JsFunction createLogger();
    }

    @JsDefinition
    interface EmptyJs extends Serializable {
    }

    @JsDefinition
    @JsCalledWith("value")
    interface CalledWithOnDefinitionJs extends Serializable {
        @JsExpression("console.log(value);")
        JsFunction createLogger();
    }

    @JsDefinition
    interface MixedJs extends Serializable {
        @JsExpression("console.log($0, value);")
        @JsCalledWith("value")
        JsFunction createLogger(String prefix);

        @JsExpression("console.log($0);")
        void log(String message);
    }

    @JsDefinition
    interface CalledWithWhenRunJs extends Serializable {
        @JsExpression("console.log(value);")
        @JsCalledWith("value")
        void log();
    }

    private final List<JsCall> calls = new ArrayList<>();

    private final PendingJavaScriptResult result = mock(
            PendingJavaScriptResult.class);

    private <T> T proxy(Class<T> definitionType) {
        return JsDefinitionProxy.create(definitionType, call -> {
            calls.add(call);
            return result;
        });
    }

    @Test
    void create_callOfADeclaredMethod_runsItAsACall() {
        GreeterJs greeter = proxy(GreeterJs.class);

        greeter.showGreeting("Hello");

        assertEquals(List.of(
                new JsCall(GreeterJs.class, "showGreeting", List.of("Hello"))),
                calls);
        assertNotNull(greeter.toString(),
                "the implementation should answer the methods of Object");
    }

    @Test
    void create_methodDeclaringAResult_answersWithWhatRanIt() {
        assertEquals(result, proxy(GreeterJs.class).readValue(),
                "a method that declares a result answers with the pending one");
    }

    @Test
    void create_notAnInterface_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> proxy(JsDefinitionProxyTest.class),
                "only an interface can declare JavaScript methods");
    }

    @Test
    void create_interfaceWithoutAnnotation_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> proxy(Serializable.class),
                "an interface the build does not collect should be refused");
    }

    @Test
    void create_methodWithoutDeclaredJavaScript_throws() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> proxy(UndeclaredJs.class));

        assertTrue(exception.getMessage().contains("undeclared"),
                "the message should name the method that declares nothing: "
                        + exception.getMessage());
        // A definition that fails the check is not remembered as checked
        assertThrows(IllegalArgumentException.class,
                () -> proxy(UndeclaredJs.class));
    }

    @Test
    void create_methodWithAnotherReturnType_throws() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> proxy(UnsupportedJs.class));

        assertTrue(exception.getMessage().contains("readValue"),
                "the message should name the method: "
                        + exception.getMessage());
    }

    @Test
    void create_methodImplementedInJava_throws() {
        // A default method and a static one are both Java on an interface that
        // is about JavaScript, so neither is quietly left aside
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> proxy(ComposingJs.class)).getMessage().contains("twice"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> proxy(HelpingJs.class)).getMessage().contains("name"));
    }

    @Test
    void createSnippets_callOfADeclaredMethod_answersWithTheFunctionItDeclares() {
        LoggerJs logger = JsDefinitionProxy.createSnippets(LoggerJs.class);

        JsFunction function = logger.createLogger("Greeting:");

        assertEquals(
                new JsCall(LoggerJs.class, "createLogger",
                        List.of("Greeting:")),
                function.getDeclaredCall(),
                "the function should name what declared it, for the client to look it up by");
        assertEquals(List.of("Greeting:"), function.getCaptures());
        assertEquals(
                new JsCall(LoggerJs.class, "createSnippet", List.of("Unit")),
                ((JsFunction) logger.createSnippet("Unit")).getDeclaredCall(),
                "a method returning any snippet should answer with a function");
        assertNotNull(logger.toString(),
                "the implementation should answer the methods of Object");
    }

    @Test
    void createSnippets_domEventSnippets_answerWithTheTypeTheyDeclare() {
        EventJs events = JsDefinitionProxy.createSnippets(EventJs.class);

        assertEquals(new JsCall(EventJs.class, "isKey", List.of("Enter")),
                events.isKey("Enter").getDeclaredCall());
        assertEquals(new JsCall(EventJs.class, "attribute", List.of("id")),
                events.attribute("id").getDeclaredCall());
    }

    @Test
    void createSnippets_domEventSnippetCaptureTheClientCanNotReceive_throwsWhenDeclared() {
        EventJs events = JsDefinitionProxy.createSnippets(EventJs.class);
        SelfReference unencodable = new SelfReference();

        assertThrows(RuntimeException.class,
                () -> events.isDetail(unencodable));
        assertThrows(RuntimeException.class, () -> events.echo(unencodable));
    }

    @Test
    void createSnippets_domEventSnippets_surviveSerialization() {
        EventJs events = JsDefinitionProxy.createSnippets(EventJs.class);
        DomEventFilter filter = events.isKey("Enter");
        DomEventData<String> data = events.attribute("id");

        DomEventFilter filterCopy = SerializationUtils.roundtrip(filter);
        DomEventData<String> dataCopy = SerializationUtils.roundtrip(data);

        assertEquals(filter.getDeclaredCall(), filterCopy.getDeclaredCall());
        assertEquals(filter.getEncodedCaptures(),
                filterCopy.getEncodedCaptures());
        // Still read by the same key, as the type the method declares
        assertEquals(ElementListenerMap.getEventDataKey(data),
                ElementListenerMap.getEventDataKey(dataCopy));
        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put(ElementListenerMap.getEventDataKey(data), "value");
        assertEquals("value",
                new DomEvent(new Element("div"), "click", eventData)
                        .getEventData(dataCopy));
    }

    @Test
    void createSnippets_methodRunningJavaScript_throws() {
        // Nothing to run it on, since a snippet is not bound to an element
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsDefinitionProxy.createSnippets(GreeterJs.class));

        assertTrue(exception.getMessage().contains("showGreeting"),
                "the message should name the method: "
                        + exception.getMessage());
    }

    @Test
    void createSnippets_noMethods_saysThereAreNoSnippets() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsDefinitionProxy.createSnippets(EmptyJs.class));

        assertTrue(exception.getMessage().endsWith("declares no snippets"),
                exception.getMessage());
    }

    @Test
    void createSnippets_calledWithANameAFunctionCanNotDeclare_throws() {
        // When the implementation is handed out, rather than when the snippet
        // is evaluated and the browser reports a syntax error
        assertThrows(IllegalArgumentException.class, () -> JsDefinitionProxy
                .createSnippets(CalledWithInvalidNameJs.class));
    }

    @Test
    void checkDefinition_calledWithOnTheDefinition_throws() {
        // The names would otherwise be ignored, and the expression would fail
        // in the browser on the first call
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> JsDefinitionProxy
                        .checkDefinition(CalledWithOnDefinitionJs.class));

        assertTrue(exception.getMessage().contains("@JsCalledWith"),
                exception.getMessage());
    }

    @Test
    void checkDefinition_snippetsBesideJavaScriptToRun_throwsForEitherUse() {
        // Whichever way it is used, half of it can not work
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsDefinitionProxy.checkDefinition(MixedJs.class));

        assertTrue(exception.getMessage().contains("log"),
                "the message should name the method that does not fit: "
                        + exception.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> JsDefinitionProxy.createSnippets(MixedJs.class));
        assertThrows(IllegalArgumentException.class,
                () -> proxy(MixedJs.class));
    }

    @Test
    void create_methodReturningASnippet_throws() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> proxy(LoggerJs.class));

        assertTrue(exception.getMessage().contains("JsSnippets.of"),
                "the message should say where a snippet is obtained from: "
                        + exception.getMessage());
    }

    @Test
    void create_calledWithOnAMethodRunningJavaScript_throws() {
        // A call runs the expression rather than a function around it, so the
        // names would never be declared
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> proxy(CalledWithWhenRunJs.class));

        assertTrue(exception.getMessage().contains("@JsCalledWith"),
                exception.getMessage());
    }
}
