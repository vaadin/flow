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

import java.io.Serializable;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsCalledWith;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
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

    @JsDefinition
    public interface JoinJs extends Serializable {

        @JsExpression("return [$0, ...$1].join('-')")
        PendingJavaScriptResult join(String first, Object... rest);
    }

    /**
     * A snippet type of the application, not a built-in one such as
     * {@code JsFunction}.
     */
    @JsCalledWith("name")
    public interface Greeting extends JsSnippet {
    }

    @JsDefinition
    public interface GreetingJs extends Serializable {

        @JsExpression("return $0 + ', ' + name;")
        Greeting createGreeting(String greeting);
    }

    @JsDefinition
    public interface ShowGreetingJs extends Serializable {

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
                        .executeJs(ShowGreetingJs.class)
                        .showGreeting(JsSnippets.of(GreetingJs.class)
                                .createGreeting("Hello")));
        runSnippet.setId(RUN_SNIPPET_ID);
        add(runSnippet, snippetResult);
    }
}
