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
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.js.JsStringFallback;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.ContentSecurityPolicyViolationException;

/**
 * Sends JavaScript as strings, the way an add-on that can not be changed does.
 * The application runs with a strict Content Security Policy, so a string runs
 * only when an interface marked with {@link JsStringFallback} declares it. The
 * build lists those interfaces in a resource, and the server loads them by name
 * and reads their methods through reflection, which a native image allows only
 * for what is registered at build time.
 */
@Route("string-fallback")
public class StringFallbackView extends Div {

    public static final String SEND_DECLARED_ID = "send-declared";
    public static final String STATUS_ID = "status";
    public static final String RETURNED_ID = "returned";
    public static final String CLICK_ID = "click";
    public static final String CLICKED_ID = "clicked";
    public static final String SEND_UNDECLARED_ID = "send-undeclared";
    public static final String UNDECLARED_RESULT_ID = "undeclared-result";

    static final String SET_STATUS = "this.textContent = $0; return this.textContent.length";

    static final String SET_TITLE = "this.title = $0";

    static final String FILTER = "event.button === 0";

    static final String EVENT_DATA = "event.type + ' from the bundle'";

    @JsDefinition
    @JsStringFallback(filters = FILTER, eventData = EVENT_DATA)
    public interface AddonFallbacks extends JsInvokers {
        @JsExpression(SET_STATUS)
        PendingJavaScriptResult setStatus(String status);
    }

    @JsDefinition
    @JsStringFallback
    public interface AddonFunctionFallbacks extends JsSnippets {
        @JsExpression(SET_TITLE)
        JsFunction setTitle(Object title);
    }

    // The string APIs are what an add-on calls
    @SuppressWarnings("deprecation")
    public StringFallbackView() {
        Span status = new Span();
        status.setId(STATUS_ID);
        Span returned = new Span();
        returned.setId(RETURNED_ID);
        NativeButton sendDeclared = new NativeButton("Send declared strings",
                event -> {
                    status.getElement().executeJs(SET_STATUS, "string ran")
                            .then(Integer.class, length -> returned
                                    .setText("returned " + length));
                    status.getElement().addJsInitializer(SET_TITLE,
                            "initialized");
                });
        sendDeclared.setId(SEND_DECLARED_ID);
        add(sendDeclared, status, returned);

        Span clicked = new Span();
        clicked.setId(CLICKED_ID);
        NativeButton click = new NativeButton("Click with a string filter");
        click.setId(CLICK_ID);
        click.getElement().addEventListener("click",
                event -> clicked.setText(
                        event.getEventData().get(EVENT_DATA).asString()))
                .setFilter(FILTER).addEventData(EVENT_DATA);
        add(click, clicked);

        Span undeclaredResult = new Span();
        undeclaredResult.setId(UNDECLARED_RESULT_ID);
        NativeButton sendUndeclared = new NativeButton(
                "Send an undeclared string", event -> {
                    try {
                        undeclaredResult.getElement()
                                .executeJs("this.textContent = 'sent'");
                    } catch (ContentSecurityPolicyViolationException e) {
                        undeclaredResult.setText("refused");
                    }
                });
        sendUndeclared.setId(SEND_UNDECLARED_ID);
        add(sendUndeclared, undeclaredResult);
    }
}
