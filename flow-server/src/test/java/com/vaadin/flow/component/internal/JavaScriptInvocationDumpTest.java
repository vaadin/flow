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
package com.vaadin.flow.component.internal;

import java.io.Serializable;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.JavaScriptInvocationDump.Call;
import com.vaadin.flow.component.internal.JavaScriptInvocationDump.Entry;
import com.vaadin.flow.component.internal.JavaScriptInvocationDump.FunctionCall;
import com.vaadin.flow.component.internal.JavaScriptInvocationDump.Script;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaScriptInvocationDumpTest {

    @JsDefinition
    interface ScrollJs extends Serializable {
        @JsExpression("this.scrollTo($0)")
        void scrollTo(int position);
    }

    private UI ui;

    private Element element;

    @BeforeEach
    void setUp() {
        ui = new MockUI();
        element = ElementFactory.createDiv();
        ui.getElement().appendChild(element);
    }

    @Test
    void mixedQueue_readInScheduledOrder() {
        element.executeJs(ScrollJs.class).scrollTo(42);
        element.callJsFunction("$connector.scrollToItem", "key", 3);
        element.executeJs("this.scrollTop = $0", 0);

        List<Entry> entries = takeFromUi().getEntries();

        assertEquals(3, entries.size());
        assertEquals(ScrollJs.class,
                assertInstanceOf(Call.class, entries.get(0)).call()
                        .definitionType());
        assertEquals(Element.CallFunctionJs.class,
                assertInstanceOf(Call.class, entries.get(1)).call()
                        .definitionType());
        Script script = assertInstanceOf(Script.class, entries.get(2),
                "plain JavaScript should be read as an expression, since it carries no call");
        assertTrue(script.expression().contains("this.scrollTop = $0"),
                "the expression should be the JavaScript the element was given");
        assertTrue(script.parameters().contains(0),
                "the parameters should be the ones the expression was given");
        assertEquals(element, script.owner());
    }

    @Test
    void getCalls_onlyTheCallsOfTheGivenDefinition() {
        element.executeJs(ScrollJs.class).scrollTo(42);
        element.callJsFunction("open");
        element.executeJs("this.scrollTop = 0");

        List<Call> calls = takeFromUi().getCalls(ScrollJs.class);

        assertEquals(1, calls.size());
        assertEquals(new JsCall(ScrollJs.class, "scrollTo", List.of(42)),
                calls.get(0).call());
        assertEquals(element, calls.get(0).owner());
    }

    @Test
    void getFunctionCalls_namesAndArgumentsOfTheCalledFunctions() {
        element.callJsFunction("$connector.setDateMetadataConfig", "config");
        element.callJsFunction("clearCache");
        element.executeJs(ScrollJs.class).scrollTo(42);

        List<FunctionCall> calls = takeFromUi().getFunctionCalls();

        assertEquals(List.of("$connector.setDateMetadataConfig", "clearCache"),
                calls.stream().map(FunctionCall::functionName).toList(),
                "only the function calls should be read, in the order they were scheduled in");
        assertEquals(List.of("config"), calls.get(0).arguments(),
                "the arguments should not repeat the name of the function");
        assertEquals(List.of(), calls.get(1).arguments());
        assertEquals(element, calls.get(0).owner());
    }

    @Test
    void queriedTwice_answeredFromWhatWasTaken() {
        element.callJsFunction("open");

        JavaScriptInvocationDump dump = takeFromUi();

        assertEquals(1, dump.getFunctionCalls().size());
        assertEquals(1, dump.getFunctionCalls().size(),
                "a second query should be answered from the dump, although the queue is empty");
        assertEquals(List.of(),
                JavaScriptInvocationDump.takeFrom(ui).getEntries(),
                "the queue should have been emptied by reading it");
    }

    private JavaScriptInvocationDump takeFromUi() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        return JavaScriptInvocationDump.takeFrom(ui);
    }
}
