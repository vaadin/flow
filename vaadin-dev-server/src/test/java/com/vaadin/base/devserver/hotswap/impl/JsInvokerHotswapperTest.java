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
package com.vaadin.base.devserver.hotswap.impl;

import java.io.Serializable;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.dom.JsExpression;
import com.vaadin.flow.dom.JsInvoker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsInvokerHotswapperTest {

    @JsInvoker
    interface GreeterJs extends Serializable {
        @JsExpression("window.alert($0)")
        void showGreeting(String greeting);
    }

    private static final String GENERATED_FOR_GREETER = """
            window.Vaadin.Flow.jsInvokers["com.acme.GreeterJs"] = Object.assign({}, {
              "showGreeting/1": async function ($0) {
            window.alert($0)
              },
            });
            """;

    @Test
    void isInBundle_declarationsAreInTheGeneratedFile() {
        assertTrue(JsInvokerHotswapper.isInBundle(GreeterJs.class,
                GENERATED_FOR_GREETER));
    }

    @Test
    void isInBundle_generatedFromAnotherVersionOfTheDeclarations() {
        // The method is still there, but with the JavaScript of before the
        // change: a call would run that, not what the interface now declares.
        assertFalse(
                JsInvokerHotswapper.isInBundle(GreeterJs.class,
                        GENERATED_FOR_GREETER.replace("window.alert($0)",
                                "window.alert('edited ' + $0)")),
                "an expression the bundle does not carry should be reported");
        // And the method missing altogether is the same answer.
        assertFalse(JsInvokerHotswapper.isInBundle(GreeterJs.class,
                GENERATED_FOR_GREETER.replace("showGreeting/1",
                        "showGreeting/2")));
    }

    @Test
    void isInBundle_noGeneratedFile() {
        assertFalse(JsInvokerHotswapper.isInBundle(GreeterJs.class, null),
                "without a generated file there is nothing carrying the declarations");
    }
}
