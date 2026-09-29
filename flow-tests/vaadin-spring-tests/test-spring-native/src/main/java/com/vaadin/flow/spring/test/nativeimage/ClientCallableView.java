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
package com.vaadin.flow.spring.test.nativeimage;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

/**
 * Calls a server method whose parameter and return value are beans, which are
 * read and written by Jackson through reflection.
 */
@Route("client-callable")
public class ClientCallableView extends Div {

    public static final String CALL_ID = "call";
    public static final String GREETING_ID = "greeting";

    public record Name(String first) {
    }

    public record Greeting(String text) {
    }

    public ClientCallableView() {
        Span greeting = new Span();
        greeting.setId(GREETING_ID);
        NativeButton call = new NativeButton("Call the server",
                event -> getElement().executeJs(
                        "this.$server.greet({first: $0}).then(result => $1.textContent = result.text)",
                        "native image", greeting));
        call.setId(CALL_ID);
        add(call, greeting);
    }

    @ClientCallable
    private Greeting greet(Name name) {
        return new Greeting("Hello, " + name.first());
    }
}
