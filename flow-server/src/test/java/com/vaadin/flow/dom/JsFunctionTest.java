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
package com.vaadin.flow.dom;

import java.io.Serializable;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.js.JsCalledWith;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsFunctionTest {

    public static class Unencodable {
        public String getValue() {
            throw new UnsupportedOperationException("Not for the client");
        }
    }

    @JsDefinition
    interface LoggerJs extends Serializable {
        @JsExpression("console.log($0, value);")
        @JsCalledWith("value")
        JsFunction createLogger(String prefix);

        @JsExpression("console.log($0, value);")
        @JsCalledWith("value")
        JsFunction createAnyLogger(Object prefix);

        @JsExpression("return (value) => console.log($0, value);")
        JsFunction createUnwrappedLogger(String prefix);
    }

    @Test
    void withArguments_alreadySet_throws() {
        JsFunction fn = JsFunction.of("return a;").withArguments("a");
        assertThrows(IllegalStateException.class, () -> fn.withArguments("b"));
    }

    @Test
    @SuppressWarnings("removal")
    void declaredFunction_hasNoBodyAndTakesNoFurtherArguments() {
        JsFunction fn = JsSnippets.of(LoggerJs.class).createLogger("Greeting:");

        assertThrows(IllegalStateException.class, fn::getBody,
                "the JavaScript is in the bundle rather than in the function");
        assertThrows(IllegalStateException.class,
                () -> fn.withArguments("other"),
                "the arguments are part of the JavaScript in the bundle");
        assertEquals(List.of("value"), fn.getArgumentNames());
        assertEquals(List.of(),
                JsSnippets.of(LoggerJs.class).createUnwrappedLogger("Greeting:")
                        .getArgumentNames(),
                "a function the expression returns declares no names");
    }

    @Test
    void declaredFunction_captureTheClientCanNotReceive_throwsWhenDeclared() {
        // Either kind of function encodes its captures when it is created, so
        // the failure points at the application code that passed the value
        // rather than at the response that would have sent it later
        LoggerJs logger = JsSnippets.of(LoggerJs.class);
        Unencodable capture = new Unencodable();

        assertThrows(RuntimeException.class,
                () -> logger.createAnyLogger(capture),
                "a declared function should refuse the capture when declared");
        assertThrows(RuntimeException.class,
                () -> JsFunction.of("console.log($0);", capture),
                "a function with a body should refuse the capture when created");
    }
}
