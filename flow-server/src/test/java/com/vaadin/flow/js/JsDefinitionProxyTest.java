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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.page.JavaScriptResult;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.component.page.PendingJavaScriptResult.JavaScriptException;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.DomEventData;
import com.vaadin.flow.dom.DomEventFilter;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.BundleUtils;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.JsStringFallbacks;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.js.internal.JsWithExecutionContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

class JsDefinitionProxyTest {

    @JsDefinition
    interface GreeterJs extends JsInvokers {
        @JsExpression("window.alert($0)")
        void showGreeting(String greeting);

        @JsExpression("return this.value;")
        PendingJavaScriptResult readValue();
    }

    public record Person(String name, int age) implements Serializable {
    }

    @JsDefinition
    interface TypedJs extends JsInvokers {
        @JsExpression("return 42;")
        JavaScriptResult<Integer> readAnswer();

        @JsExpression("return [{name: 'Ada', age: 36}];")
        JavaScriptResult<List<Person>> readPeople();

        @JsExpression("return this.dataset;")
        JavaScriptResult<JsonNode> readJson();
    }

    interface ValueJs<T> extends Serializable {
        @JsExpression("return this.value;")
        JavaScriptResult<T> readValue();
    }

    interface ListJs<T> extends ValueJs<List<T>> {
    }

    interface ArrayJs<T> extends ValueJs<T[]> {
    }

    @JsDefinition
    interface CounterJs extends JsInvokers, ValueJs<Integer> {
    }

    @JsDefinition
    interface NumbersJs extends JsInvokers, ListJs<Integer> {
    }

    @JsDefinition
    interface IntsJs extends JsInvokers, ArrayJs<Integer> {
    }

    @SuppressWarnings("rawtypes")
    @JsDefinition
    interface RawValueJs extends JsInvokers, ValueJs {
    }

    @JsDefinition
    interface StillGenericJs<T> extends JsInvokers, ValueJs<T> {
    }

    interface UnusedVariableJs<U> extends ValueJs<Integer> {
    }

    @SuppressWarnings("rawtypes")
    @JsDefinition
    interface RawUnusedVariableJs extends JsInvokers, UnusedVariableJs {
    }

    @JsDefinition
    interface UntypedJs<T> extends JsInvokers {
        @SuppressWarnings("rawtypes")
        @JsExpression("return 1;")
        JavaScriptResult readRaw();

        @JsExpression("return 1;")
        JavaScriptResult<?> readWildcard();

        @JsExpression("return [1];")
        JavaScriptResult<List<T>> readVariable();
    }

    // Does not extend JsInvokers, so a method without @JsExpression declares
    // nothing even when a definition mixes it in
    interface UndeclaredMixin {
        void undeclared();
    }

    @JsDefinition
    interface UndeclaredJs extends JsInvokers, UndeclaredMixin {
    }

    @JsDefinition
    interface UndeclaredSnippetsJs extends JsSnippets {
        JsFunction undeclared();
    }

    // Calls the method of the same name on the element
    @JsDefinition
    interface ShorthandJs extends JsInvokers {
        void showPopover(String source);

        PendingJavaScriptResult checkVisibility(String... options);
    }

    @JsDefinition
    @JsStringFallback
    interface ShorthandFallbacks extends JsInvokers {
        void focus();
    }

    @JsDefinition
    interface UnsupportedJs extends JsInvokers {
        @JsExpression("return this.value;")
        String readValue();
    }

    @JsDefinition
    interface ComposingJs extends JsInvokers {
        @JsExpression("this.method()")
        void method();

        default void twice() {
            method();
            method();
        }
    }

    @JsDefinition
    interface HelpingJs extends JsInvokers {
        @JsExpression("this.method()")
        void method();

        static String name() {
            return "HelpingJs";
        }
    }

    @JsDefinition
    interface LoggerJs extends JsSnippets {
        @JsExpression("console.log($0, value);")
        @JsFunctionParameterNames("value")
        JsFunction createLogger(String prefix);

        @JsExpression("return (value) => console.log($0, value);")
        JsSnippet createSnippet(String prefix);
    }

    @JsFunctionParameterNames({ "html", "item" })
    interface TemplateSnippet extends JsSnippet {
        // Allowed, as it is not something the snippet has to implement
        static String describe() {
            return "template";
        }
    }

    @JsDefinition
    interface TemplateJs extends JsSnippets {
        @JsExpression("return html`<b>${$0}: ${item.name}</b>`;")
        TemplateSnippet label(String prefix);

        @JsExpression("return html`${$0}`;")
        TemplateSnippet echo(Object value);

        @JsExpression("return html`${$0.join(' ')}`;")
        TemplateSnippet join(String... parts);
    }

    @JsDefinition
    interface OtherTemplateJs extends JsSnippets {
        @JsExpression("return html`<i>${$0}: ${item.name}</i>`;")
        TemplateSnippet label(String prefix);
    }

    interface ActionSnippet extends JsSnippet {
        void run();

        default void runTwice() {
            run();
            run();
        }
    }

    @JsDefinition
    interface ActionJs extends JsSnippets {
        @JsExpression("console.log('run');")
        ActionSnippet createAction();
    }

    @JsDefinition
    interface EventJs extends JsSnippets {
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
    interface FunctionParameterNamesInvalidNameJs extends JsSnippets {
        @JsExpression("console.log(value);")
        @JsFunctionParameterNames("not-a-name")
        JsFunction createLogger();
    }

    @JsDefinition
    interface SnippetAmongCallsJs extends JsInvokers {
        @JsExpression("console.log(value);")
        JsFunction createLogger();
    }

    static class GreeterDouble implements JsInvokers {
    }

    // Only an interface can be implemented by a proxy
    @JsDefinition
    static class AnnotatedDouble implements JsInvokers {
    }

    @JsDefinition
    interface BothKindsJs extends JsInvokers, JsSnippets {
        @JsExpression("console.log($0);")
        void log(String message);
    }

    // Not found by the build, so its JavaScript would not be in the bundle
    interface UnannotatedJs extends JsInvokers {
        @JsExpression("console.log($0);")
        void log(String message);
    }

    @JsFunctionParameterNames("value")
    @JsDefinition
    interface FunctionParameterNamesOnDefinitionJs extends JsSnippets {
        @JsExpression("console.log(value);")
        JsFunction createLogger();
    }

    @JsDefinition
    interface MixedJs extends JsSnippets {
        @JsExpression("console.log($0, value);")
        @JsFunctionParameterNames("value")
        JsFunction createLogger(String prefix);

        @JsExpression("console.log($0);")
        void log(String message);
    }

    @JsDefinition
    interface FunctionParameterNamesWhenRunJs extends JsInvokers {
        @JsExpression("console.log(value);")
        @JsFunctionParameterNames("value")
        void log();
    }

    @JsDefinition
    interface SnippetWithExecutionContextJs extends JsSnippets {
        @JsExpression("console.log($executionContext);")
        @JsWithExecutionContext
        JsFunction createLogger();
    }

    @JsDefinition
    @JsDefinitionImport(from = "./grid.js", defaultExport = "grid")
    @JsDefinitionImport(from = "./grid.js", namespace = "gridModule", exports = {
            "init", "refresh" })
    @JsDefinitionImport(from = "@vaadin/tooltip", exports = "Tooltip")
    interface ImportingJs extends JsInvokers {
        @JsExpression("grid.init(this, Tooltip)")
        void init();
    }

    @JsDefinitionImport(from = "./grid.js", namespace = "inherited")
    interface ImportingParentJs extends JsInvokers {
        @JsExpression("inherited.refresh(this)")
        void refresh();
    }

    @JsDefinition
    interface ImportingChildJs extends ImportingParentJs {
    }

    @JsDefinition
    @JsDefinitionImport(from = "./list.js", namespace = "inherited")
    interface ImportingOtherModuleJs extends JsInvokers {
        @JsExpression("inherited.refresh(this)")
        void refresh();
    }

    @JsDefinition
    @JsDefinitionImport(from = "./grid.js", namespace = "grid")
    interface ImportingElementMethodJs extends JsInvokers {
        void refresh(String value);
    }

    @JsDefinition
    interface ElementMethodJs extends JsInvokers {
        void refresh(String value);
    }

    @JsDefinition
    @JsDefinitionImport(from = "./grid.js")
    interface ImportWithoutNamesOnlyInheritingJs extends ImportingParentJs {
    }

    @JsDefinition
    @JsDefinitionImport(from = "./grid.js")
    interface ImportWithoutNamesJs extends JsInvokers {
        @JsExpression("this.refresh()")
        void refresh();
    }

    @JsDefinition
    @JsDefinitionImport(from = "./grid.js", defaultExport = "this")
    interface ImportOfReservedWordJs extends JsInvokers {
        @JsExpression("this.refresh()")
        void refresh();
    }

    @JsDefinition
    @JsDefinitionImport(from = "./grid.js", exports = "grid-connector")
    interface ImportOfNonIdentifierJs extends JsInvokers {
        @JsExpression("this.refresh()")
        void refresh();
    }

    @JsDefinition
    @JsDefinitionImport(from = "./grid.js", namespace = "$0")
    interface ImportOfArgumentNameJs extends JsInvokers {
        @JsExpression("this.refresh($0)")
        void refresh(String value);
    }

    @JsDefinition
    @JsDefinitionImport(from = "./grid.js", namespace = "$executionContext")
    interface ImportOfExecutionContextNameJs extends JsInvokers {
        @JsExpression("this.refresh()")
        void refresh();
    }

    @JsDefinition
    @JsDefinitionImport(from = "./grid.js", defaultExport = "grid", namespace = "grid")
    interface ImportOfNameTwiceJs extends JsInvokers {
        @JsExpression("grid.refresh()")
        void refresh();
    }

    @JsDefinition
    @JsDefinitionImport(from = "./grid.js", exports = "refresh")
    @JsDefinitionImport(from = "./list.js", exports = "refresh")
    interface ImportOfNameFromTwoModulesJs extends JsInvokers {
        @JsExpression("refresh()")
        void refresh();
    }

    @JsDefinition
    @JsDefinitionImport(from = "./grid.js", namespace = "grid")
    interface ImportingSnippetsJs extends JsSnippets {
        @JsExpression("grid.refresh()")
        JsFunction refresh();
    }

    @JsDefinition
    interface InheritingImportSnippetsJs
            extends JsSnippets, ImportingSnippetsParentJs {
    }

    @JsDefinitionImport(from = "./grid.js", namespace = "grid")
    interface ImportingSnippetsParentJs {
        @JsExpression("grid.refresh()")
        JsFunction refresh();
    }

    @JsDefinition
    @JsStringFallback
    @JsDefinitionImport(from = "./grid.js", namespace = "grid")
    interface ImportingFallbacks extends JsInvokers {
        @JsExpression("grid.refresh()")
        void refresh();
    }

    private final List<JsCall> calls = new ArrayList<>();

    private final RecordingResult result = new RecordingResult();

    /** A pending result that keeps its handlers for a test to complete. */
    private static class RecordingResult implements PendingJavaScriptResult {
        private final List<SerializableConsumer<JsonNode>> resultHandlers = new ArrayList<>();
        private final List<SerializableConsumer<String>> errorHandlers = new ArrayList<>();

        @Override
        public boolean cancelExecution() {
            return false;
        }

        @Override
        public boolean isSentToBrowser() {
            return false;
        }

        @Override
        public void then(SerializableConsumer<JsonNode> resultHandler,
                SerializableConsumer<String> errorHandler) {
            resultHandlers.add(resultHandler);
            if (errorHandler != null) {
                errorHandlers.add(errorHandler);
            }
        }

        private void complete(String json) {
            JsonNode value = JacksonUtils.getMapper().readTree(json);
            resultHandlers.forEach(handler -> handler.accept(value));
        }

        private void fail(String message) {
            errorHandlers.forEach(handler -> handler.accept(message));
        }
    }

    // Raw, so that a type the signature refuses can still reach the check
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> T proxy(Class<T> definitionType) {
        return (T) JsDefinitionProxy.create((Class) definitionType, call -> {
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
        assertSame(result, proxy(GreeterJs.class).readValue(),
                "a method that declares a result answers with the pending one");
        assertSame(result, proxy(TypedJs.class).readJson(),
                "the pending result already is a result of JSON");
    }

    @Test
    void create_methodDeclaringATypedResult_convertsTheValue() {
        List<Person> people = new ArrayList<>();
        JavaScriptResult<List<Person>> typed = proxy(TypedJs.class)
                .readPeople();
        assertTrue(result.resultHandlers.isEmpty(),
                "a result nobody reads should not be sent back");

        typed.then(people::addAll);
        result.complete("[{\"name\":\"Ada\",\"age\":36}]");

        assertEquals(List.of(new Person("Ada", 36)), people);
    }

    @Test
    void create_methodDeclaringATypedResult_completesTheFutureWithTheValue() {
        CompletableFuture<Integer> answer = proxy(TypedJs.class).readAnswer()
                .toCompletableFuture();

        result.complete("42");

        assertEquals(42, answer.getNow(null));
    }

    @Test
    void create_methodDeclaringATypedResult_failsTheFutureWithTheError() {
        CompletableFuture<Integer> answer = proxy(TypedJs.class).readAnswer()
                .toCompletableFuture();

        result.fail("not thinking");

        CompletionException exception = assertThrows(CompletionException.class,
                () -> answer.getNow(null));
        assertInstanceOf(JavaScriptException.class, exception.getCause());
        assertEquals("not thinking", exception.getCause().getMessage());
    }

    @Test
    void create_valueThatCanNotBeConverted_failsWithTheConversionError() {
        List<String> errors = new ArrayList<>();
        JavaScriptResult<Integer> answer = proxy(TypedJs.class).readAnswer();
        answer.then(value -> errors.add("converted " + value), errors::add);
        CompletableFuture<Integer> future = answer.toCompletableFuture();

        result.complete("\"forty-two\"");

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Integer"), errors.get(0));
        CompletionException exception = assertThrows(CompletionException.class,
                () -> future.getNow(null));
        JavaScriptException javaScriptException = assertInstanceOf(
                JavaScriptException.class, exception.getCause());
        assertInstanceOf(IllegalArgumentException.class,
                javaScriptException.getCause());
    }

    @Test
    void create_valueThatCanNotBeConvertedWithoutAnErrorHandler_runsLaterHandlers() {
        JavaScriptResult<Integer> answer = proxy(TypedJs.class).readAnswer();
        answer.then(value -> {
        });
        CompletableFuture<Integer> future = answer.toCompletableFuture();

        result.complete("\"forty-two\"");

        assertThrows(CompletionException.class, () -> future.getNow(null));
    }

    @Test
    void create_typedResultFromAGenericInterface_convertsToTheBoundType() {
        List<Integer> counts = new ArrayList<>();
        proxy(CounterJs.class).readValue().then(counts::add);
        result.complete("7");
        assertEquals(List.of(7), counts);

        result.resultHandlers.clear();
        List<Integer> numbers = new ArrayList<>();
        proxy(NumbersJs.class).readValue().then(numbers::addAll);
        result.complete("[4, 2]");
        assertEquals(List.of(4, 2), numbers);

        result.resultHandlers.clear();
        List<Integer> ints = new ArrayList<>();
        proxy(IntsJs.class).readValue()
                .then(array -> ints.addAll(List.of(array)));
        result.complete("[4, 2]");
        assertEquals(List.of(4, 2), ints);
    }

    @Test
    void create_methodDeclaringATypedResult_isSerializable() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(proxy(TypedJs.class).readPeople());
        }
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            assertInstanceOf(JavaScriptResult.class, in.readObject());
        }
    }

    @Test
    void create_typedResultWithoutAHandler_throws() {
        JavaScriptResult<Integer> answer = proxy(TypedJs.class).readAnswer();

        assertThrows(IllegalArgumentException.class, () -> answer.then(null));
    }

    @Test
    void create_typedResultWithoutAConcreteType_throws() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> proxy(UntypedJs.class));

        String message = exception.getMessage();
        assertTrue(
                message.contains("readRaw") && message.contains("readWildcard")
                        && message.contains("readVariable"),
                "the message should name every method: " + message);
        assertTrue(message.contains("PendingJavaScriptResult"),
                "the message should say what to declare instead: " + message);
        for (Class<?> unbound : List.of(RawValueJs.class, StillGenericJs.class,
                RawUnusedVariableJs.class)) {
            String unboundMessage = assertThrows(IllegalArgumentException.class,
                    () -> proxy(unbound)).getMessage();
            assertTrue(unboundMessage.contains("readValue"),
                    unbound.getSimpleName()
                            + " leaves the type variable unbound: "
                            + unboundMessage);
        }
    }

    @Test
    void create_notAnInterface_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> proxy(JsDefinitionProxyTest.class),
                "only an interface can declare JavaScript methods");
    }

    @Test
    void create_interfaceExtendingNeitherMarker_throws() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> proxy(Serializable.class),
                "an interface the build does not collect should be refused");

        assertTrue(exception.getMessage().contains("neither"),
                exception.getMessage());
        assertTrue(exception.getMessage().contains("JsInvokers"),
                exception.getMessage());
        assertTrue(exception.getMessage().contains("JsSnippets"),
                exception.getMessage());
    }

    @Test
    void checkDefinition_snippetAmongJavaScriptToRun_throws() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> JsDefinitionProxy
                        .checkDefinition(SnippetAmongCallsJs.class));

        assertTrue(exception.getMessage().contains("createLogger"),
                exception.getMessage());
        assertTrue(exception.getMessage().contains("JsInvokers"),
                exception.getMessage());
    }

    @Test
    void create_interfaceWithoutJsDefinition_throws() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> proxy(UnannotatedJs.class),
                "an interface the build does not find should be refused");

        assertTrue(exception.getMessage().contains("UnannotatedJs"),
                exception.getMessage());
        assertTrue(exception.getMessage().contains("@JsDefinition"),
                exception.getMessage());
    }

    @Test
    void isDefinition_onlyAnnotatedInterfaces() {
        assertTrue(JsDefinitionProxy.isDefinition(GreeterJs.class));
        assertTrue(JsDefinitionProxy.isDefinition(LoggerJs.class));
        assertFalse(JsDefinitionProxy.isDefinition(UnannotatedJs.class));
        assertFalse(JsDefinitionProxy.isDefinition(JsInvokers.class));
        assertFalse(JsDefinitionProxy.isDefinition(JsSnippets.class));
        assertFalse(JsDefinitionProxy.isDefinition(GreeterDouble.class));
        assertFalse(JsDefinitionProxy.isDefinition(AnnotatedDouble.class));
        assertFalse(JsDefinitionProxy.isDefinition(Serializable.class));
    }

    @Test
    void checkDefinition_interfaceExtendingBothMarkers_throws() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsDefinitionProxy.checkDefinition(BothKindsJs.class));

        assertTrue(exception.getMessage().contains("BothKindsJs"),
                exception.getMessage());
        assertTrue(exception.getMessage().contains("both"),
                exception.getMessage());
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
    void createSnippets_methodWithoutExpression_throws() {
        // Snippets are not called on the element, so there is no shorthand
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> JsDefinitionProxy
                        .createSnippets(UndeclaredSnippetsJs.class));

        assertTrue(exception.getMessage().contains("undeclared"),
                exception.getMessage());
        assertFalse(exception.getMessage().contains("JsInvokers"),
                "a snippet can only be declared with @JsExpression: "
                        + exception.getMessage());
    }

    @Test
    void create_methodsWithoutExpression_runAsCallsOfTheElementMethod() {
        try (MockedStatic<BundleUtils> bundleUtils = mockStatic(
                BundleUtils.class, CALLS_REAL_METHODS)) {
            ShorthandJs shorthand = JsDefinitionProxy.create(ShorthandJs.class,
                    call -> {
                        calls.add(call);
                        return result;
                    });

            shorthand.showPopover("menu");

            assertEquals(List.of(new JsCall(ShorthandJs.class, "showPopover",
                    List.of("menu"))), calls);
            // Checked against the bundle like any declared JavaScript
            assertEquals(Set.of(
                    JsCall.functionId("return this.showPopover($0);", 1, false),
                    JsCall.functionId("return this.checkVisibility(...$0);", 1,
                            true)),
                    captureCheckedIds(bundleUtils, ShorthandJs.class));
        }
    }

    @Test
    void create_stringFallbackMethodWithoutExpression_throws() {
        // A string is matched by the text of the declared expression
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> proxy(ShorthandFallbacks.class));

        assertTrue(exception.getMessage().contains("focus"),
                exception.getMessage());
        assertTrue(exception.getMessage().contains("@JsStringFallback"),
                exception.getMessage());
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
    void createSnippets_snippetTypeOfTheApplication_answersWithAnImplementationOfIt() {
        TemplateJs templates = JsDefinitionProxy
                .createSnippets(TemplateJs.class);

        TemplateSnippet label = templates.label("Name");

        assertEquals(new JsCall(TemplateJs.class, "label", List.of("Name")),
                JsDefinitionProxy.findDeclaredCall(label),
                "the snippet should name what declared it, for the client to look it up by");
        assertEquals(templates.label("Name"), label);
        assertEquals(templates.label("Name").hashCode(), label.hashCode());
        assertNotEquals(templates.label("Other"), label);
        assertNotEquals(templates.echo("Name"), label);
        assertNotEquals(
                JsDefinitionProxy.createSnippets(OtherTemplateJs.class)
                        .label("Name"),
                label,
                "a method of the same name in another definition declares another function");
        // Including a proxy that is not a snippet, such as the definition
        for (Object other : Arrays.asList(null, JsFunction.of("return 1"),
                templates)) {
            assertNotEquals((Object) label, other,
                    "a snippet should not be equal to " + other);
        }
        assertTrue(label.toString().contains("TemplateSnippet"),
                label.toString());
        assertEquals(label, SerializationUtils.roundtrip(label));
        SelfReference unencodable = new SelfReference();
        assertThrows(RuntimeException.class, () -> templates.echo(unencodable),
                "a capture the client can not receive should fail when declared");
    }

    @Test
    void createSnippets_snippetTypeOfTheApplication_equalWhenArgumentsHaveEqualContents() {
        TemplateJs templates = JsDefinitionProxy
                .createSnippets(TemplateJs.class);

        TemplateSnippet joined = templates.join("a", "b");
        assertEquals(templates.join("a", "b"), joined,
                "trailing arguments should be compared by their contents");
        assertEquals(templates.join("a", "b").hashCode(), joined.hashCode());
        assertNotEquals(templates.join("a", "c"), joined);
        assertEquals(joined, SerializationUtils.roundtrip(joined));

        TemplateSnippet echoed = templates.echo(new String[] { "a" });
        assertEquals(templates.echo(new String[] { "a" }), echoed,
                "an array argument should be compared by its contents");
        assertEquals(templates.echo(new String[] { "a" }).hashCode(),
                echoed.hashCode());
        assertNotEquals(templates.echo(new String[] { "b" }), echoed);
    }

    @Test
    void checkDefinition_snippetTypeOfTheApplicationWithMethods_throws() {
        // Nothing could implement them, as the snippet only exists on the
        // client
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsDefinitionProxy.checkDefinition(ActionJs.class));

        assertTrue(
                exception.getMessage().contains(ActionSnippet.class.getName())
                        && exception.getMessage().contains("run, runTwice"),
                exception.getMessage());
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
    void createSnippets_plainCaptures_equalKeysForEqualValues() {
        EventJs events = JsDefinitionProxy.createSnippets(EventJs.class);

        assertEquals(events.echo(42).getSnippetKey(),
                events.echo(42).getSnippetKey());
        assertEquals(events.echo(new BigDecimal("1.5")).getSnippetKey(),
                events.echo(new BigDecimal("1.5")).getSnippetKey());
        // A number that can change later is not a plain value
        AtomicInteger counter = new AtomicInteger(42);
        assertNotEquals(events.echo(counter).getSnippetKey(),
                events.echo(counter).getSnippetKey());
    }

    @Test
    void createSnippets_domEventSnippets_surviveSerialization() {
        EventJs events = JsDefinitionProxy.createSnippets(EventJs.class);
        DomEventFilter filter = events.isKey("Enter");
        DomEventData<String> data = events.attribute("id");
        // An element capture has a random key, which must be kept
        DomEventData<Object> elementData = events.echo(new Element("span"));

        DomEventFilter filterCopy = SerializationUtils.roundtrip(filter);
        DomEventData<String> dataCopy = SerializationUtils.roundtrip(data);
        DomEventData<Object> elementDataCopy = SerializationUtils
                .roundtrip(elementData);

        assertEquals(filter.getDeclaredCall(), filterCopy.getDeclaredCall());
        assertEquals(filter.getSnippetKey(), filterCopy.getSnippetKey());
        assertEquals(ElementListenerMap.getEventDataKey(elementData),
                ElementListenerMap.getEventDataKey(elementDataCopy));
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
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void createSnippets_rawInvokersType_throws() {
        // The signature refuses it, so only a raw class gets this far
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> JsDefinitionProxy
                        .createSnippets((Class) GreeterJs.class));

        assertTrue(exception.getMessage().contains("GreeterJs"),
                exception.getMessage());
        assertTrue(exception.getMessage().contains("JsSnippets"),
                exception.getMessage());
    }

    @Test
    void createSnippets_parameterNameAFunctionCanNotDeclare_throws() {
        // When the implementation is handed out, rather than when the snippet
        // is evaluated and the browser reports a syntax error
        assertThrows(IllegalArgumentException.class, () -> JsDefinitionProxy
                .createSnippets(FunctionParameterNamesInvalidNameJs.class));
    }

    @Test
    void checkDefinition_functionParameterNamesOnTheDefinition_throws() {
        // The names would otherwise be ignored, and the expression would fail
        // in the browser on the first call
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsDefinitionProxy.checkDefinition(
                        FunctionParameterNamesOnDefinitionJs.class));

        assertTrue(exception.getMessage().contains("@JsFunctionParameterNames"),
                exception.getMessage());
    }

    @Test
    void checkDefinition_javaScriptToRunAmongSnippets_throws() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsDefinitionProxy.checkDefinition(MixedJs.class));

        assertTrue(exception.getMessage().contains("log"),
                "the message should name the method that does not fit: "
                        + exception.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> JsDefinitionProxy.createSnippets(MixedJs.class));
    }

    @Test
    void create_rawSnippetsType_throws() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> proxy(LoggerJs.class));

        assertTrue(exception.getMessage().contains("LoggerJs"),
                exception.getMessage());
        assertTrue(exception.getMessage().contains("JsInvokers"),
                exception.getMessage());
    }

    @Test
    void create_functionParameterNamesOnAMethodRunningJavaScript_throws() {
        // A call runs the expression rather than a function around it, so the
        // names would never be declared
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> proxy(FunctionParameterNamesWhenRunJs.class));

        assertTrue(exception.getMessage().contains("@JsFunctionParameterNames"),
                exception.getMessage());
    }

    @Test
    void createSnippets_executionContextOnASnippet_throws() {
        // Only a call runs against an element with the context beside it, a
        // snippet is evaluated by whatever it is passed to
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> JsDefinitionProxy
                        .createSnippets(SnippetWithExecutionContextJs.class));

        assertTrue(exception.getMessage().contains("@JsWithExecutionContext"),
                exception.getMessage());
    }

    @Test
    void createAndCreateSnippets_checkEveryFunctionIsBundled()
            throws NoSuchMethodException {
        try (MockedStatic<BundleUtils> bundleUtils = mockStatic(
                BundleUtils.class, CALLS_REAL_METHODS)) {
            JsDefinitionProxy.create(GreeterJs.class, call -> null);
            JsDefinitionProxy.createSnippets(TemplateJs.class);

            assertEquals(
                    Set.of(JsCall.functionId("window.alert($0)", 1, false),
                            JsCall.functionId("return this.value;", 0, false)),
                    captureCheckedIds(bundleUtils, GreeterJs.class));
            assertEquals(
                    Set.of(JsCall.functionId(
                            TemplateJs.class.getMethod("label", String.class)),
                            JsCall.functionId(TemplateJs.class.getMethod("echo",
                                    Object.class)),
                            JsCall.functionId(TemplateJs.class.getMethod("join",
                                    String[].class))),
                    captureCheckedIds(bundleUtils, TemplateJs.class));
        }
    }

    @JsDefinition
    @JsStringFallback
    public interface FocusFallbacks extends JsInvokers {
        @JsExpression("this.focus()")
        void focus();
    }

    @Test
    void create_stringFallback_checksTheIdsTheCallSends()
            throws NoSuchMethodException {
        try (MockedStatic<BundleUtils> bundleUtils = mockStatic(
                BundleUtils.class, CALLS_REAL_METHODS)) {
            JsDefinitionProxy.create(FocusFallbacks.class, call -> null);

            // The build registers the function under the identifier of a
            // string fallback, not under that of its JavaScript
            Method focus = FocusFallbacks.class.getMethod("focus");
            String fallbackId = JsStringFallbacks
                    .functionId(FocusFallbacks.class, focus);
            assertNotEquals(JsCall.functionId(focus), fallbackId);
            assertEquals(Set.of(fallbackId),
                    captureCheckedIds(bundleUtils, FocusFallbacks.class));
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<String> captureCheckedIds(
            MockedStatic<BundleUtils> bundleUtils, Class<?> definition) {
        ArgumentCaptor<Supplier<Collection<String>>> ids = ArgumentCaptor
                .forClass(Supplier.class);
        bundleUtils.verify(() -> BundleUtils
                .checkJsFunctionsBundled(eq(definition), ids.capture()));
        return Set.copyOf(ids.getValue().get());
    }

    @Test
    void checkDefinition_importsMergedAndInherited_pass() {
        assertEquals(JsDefinitionProxy.Kind.CALLS,
                JsDefinitionProxy.checkDefinition(ImportingJs.class));
        assertEquals(JsDefinitionProxy.Kind.CALLS,
                JsDefinitionProxy.checkDefinition(ImportingChildJs.class));
    }

    static Stream<Arguments> refusedImports() {
        return Stream.of(Arguments.of(ImportWithoutNamesJs.class, "@JsModule"),
                // Declares no method of its own to find the import through
                Arguments.of(ImportWithoutNamesOnlyInheritingJs.class,
                        "@JsModule"),
                Arguments.of(ImportOfReservedWordJs.class, "'this'"),
                Arguments.of(ImportOfNonIdentifierJs.class, "'grid-connector'"),
                Arguments.of(ImportOfArgumentNameJs.class, "'$0'"),
                Arguments.of(ImportOfExecutionContextNameJs.class,
                        "'$executionContext'"),
                Arguments.of(ImportOfNameTwiceJs.class,
                        "'grid' more than once"),
                Arguments.of(ImportOfNameFromTwoModulesJs.class,
                        "'refresh' more than once"),
                Arguments.of(ImportingSnippetsJs.class, "JsInvokers"),
                Arguments.of(InheritingImportSnippetsJs.class,
                        ImportingSnippetsParentJs.class.getName()),
                Arguments.of(ImportingFallbacks.class, "@JsStringFallback"));
    }

    @ParameterizedTest
    @MethodSource("refusedImports")
    void checkDefinition_importThatCanNotBeGenerated_throws(Class<?> definition,
            String expectedInMessage) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsDefinitionProxy.checkDefinition(definition));

        assertTrue(exception.getMessage().contains("@JsDefinitionImport"),
                exception.getMessage());
        assertTrue(exception.getMessage().contains(expectedInMessage),
                exception.getMessage());
    }

    @Test
    void elementMethodWithImports_isAnotherFunctionOfTheElementMethod()
            throws NoSuchMethodException {
        assertEquals(JsDefinitionProxy.Kind.CALLS, JsDefinitionProxy
                .checkDefinition(ImportingElementMethodJs.class));
        Method importing = ImportingElementMethodJs.class.getMethod("refresh",
                String.class);
        assertEquals("return this.refresh($0);",
                JsCall.declaredExpression(importing));
        assertNotEquals(JsCall.functionId(
                ElementMethodJs.class.getMethod("refresh", String.class)),
                JsCall.functionId(importing));
    }

    @Test
    void functionId_imports_tellApartTheSameJavaScript()
            throws NoSuchMethodException {
        String importing = JsCall
                .functionId(ImportingParentJs.class.getMethod("refresh"));
        String plain = JsCall.functionId("inherited.refresh(this)", 0, false);
        assertNotEquals(plain, importing);
        assertNotEquals(
                JsCall.functionId(
                        ImportingOtherModuleJs.class.getMethod("refresh")),
                importing);
        // Called through the child, the method still has its parent's imports
        assertEquals(importing,
                JsCall.functionId(ImportingChildJs.class.getMethod("refresh")));
    }
}
