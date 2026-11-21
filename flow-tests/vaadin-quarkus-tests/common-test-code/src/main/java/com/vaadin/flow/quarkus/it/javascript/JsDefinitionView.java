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
package com.vaadin.flow.quarkus.it.javascript;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsFunctionParameterNames;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsSnippet;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.Route;

/**
 * Runs JavaScript from definitions of the application.
 * {@code Element.executeJs(Class)} and {@code JsSnippets.of(Class)} implement a
 * definition with a JDK dynamic proxy, and so does a definition method that
 * returns a snippet type of the application. A native image has such a proxy
 * only when it is registered at build time.
 */
@Route("js-definition")
public class JsDefinitionView extends Div {

    public static final String RUN_ID = "run";
    public static final String RESULT_ID = "result";
    public static final String RUN_SNIPPET_ID = "run-snippet";
    public static final String SNIPPET_RESULT_ID = "snippet-result";
    public static final String CHECK_INHERITED_METHOD_ID = "check-inherited-method";
    public static final String INHERITED_METHOD_RESULT_ID = "inherited-method-result";

    public interface JoinJs extends JsInvokers {

        @JsExpression("return [$0, ...$1].join('-')")
        PendingJavaScriptResult join(String first, Object... rest);
    }

    /**
     * A snippet type of the application that other snippet types extend.
     */
    public interface Message extends JsSnippet {
    }

    /**
     * A snippet type of the application, not a built-in one such as
     * {@code JsFunction}. It extends another snippet type, so the check that it
     * declares no methods also reads the interfaces it extends.
     */
    @JsFunctionParameterNames("name")
    public interface Greeting extends Message {
    }

    /**
     * Declares a method, which a snippet type can not do.
     */
    public interface Labeled extends JsSnippet {
        String label();
    }

    /**
     * Declares no method itself, but inherits one.
     */
    public interface LabeledGreeting extends Labeled {
    }

    public interface LabeledGreetingJs extends JsSnippets {

        @JsExpression("return $0;")
        LabeledGreeting createGreeting(String greeting);
    }

    public interface GreetingJs extends JsSnippets {

        @JsExpression("return $0 + ', ' + name;")
        Greeting createGreeting(String greeting);
    }

    public interface ShowGreetingJs extends JsInvokers {

        @JsExpression("this.textContent = $0('native image');")
        void showGreeting(Greeting greeting);
    }

    public JsDefinitionView() {
        Span result = new Span();
        result.setId(RESULT_ID);
        NativeButton run = new NativeButton("Run the definition",
                event -> getElement().executeJs(JoinJs.class).join("a", 1, true)
                        .then(String.class, result::setText));
        run.setId(RUN_ID);
        add(run, result);

        Span snippetResult = new Span();
        snippetResult.setId(SNIPPET_RESULT_ID);
        NativeButton runSnippet = new NativeButton("Run the snippet",
                event -> snippetResult.getElement()
                        .executeJs(ShowGreetingJs.class).showGreeting(JsSnippets
                                .of(GreetingJs.class).createGreeting("Hello")));
        runSnippet.setId(RUN_SNIPPET_ID);
        add(runSnippet, snippetResult);

        Span inheritedMethodResult = new Span();
        inheritedMethodResult.setId(INHERITED_METHOD_RESULT_ID);
        NativeButton checkInheritedMethod = new NativeButton(
                "Check a snippet type with an inherited method", event -> {
                    try {
                        JsSnippets.of(LabeledGreetingJs.class);
                        inheritedMethodResult.setText("accepted");
                    } catch (IllegalArgumentException e) {
                        inheritedMethodResult.setText(e.getMessage());
                    }
                });
        checkInheritedMethod.setId(CHECK_INHERITED_METHOD_ID);
        add(checkInheritedMethod, inheritedMethodResult);
    }
}
