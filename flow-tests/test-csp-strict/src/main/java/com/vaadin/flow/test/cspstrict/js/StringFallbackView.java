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
package com.vaadin.flow.test.cspstrict.js;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsStringFallback;
import com.vaadin.flow.router.Route;

/**
 * Sends JavaScript as strings, the way an add-on that can not be changed does,
 * and declares each string with {@link JsStringFallback}: the strings run from
 * the bundle, so the browser compiles nothing.
 */
@Route("string-fallback")
public class StringFallbackView extends Div {

    static final String FILTER = "event.detail >= 0 && event.button === 0";

    static final String EVENT_DATA = "event.type + ' from the bundle'";

    static final String SET_STATUS = "this.textContent = $0 + ' ' + $1;"
            + " return this.textContent.length";

    static final String SET_TITLE = "this.title = $0";

    @JsDefinition
    @JsStringFallback(filters = FILTER, eventData = EVENT_DATA)
    public interface AddonFallbacks {
        @JsExpression(SET_STATUS)
        PendingJavaScriptResult setStatus(String first, String second);
    }

    @JsDefinition
    @JsStringFallback
    public interface AddonFunctionFallbacks {
        @JsExpression(SET_TITLE)
        JsFunction setTitle(Object title);
    }

    // The string APIs are what an add-on calls
    @SuppressWarnings("deprecation")
    public StringFallbackView() {
        Span status = new Span("waiting");
        status.setId("status");
        Span returned = new Span();
        returned.setId("returned");
        status.getElement().executeJs(SET_STATUS, "string JavaScript", "ran")
                .then(Integer.class,
                        length -> returned.setText("returned " + length));
        status.getElement().addJsInitializer(SET_TITLE, "initialized");

        Span clicked = new Span();
        clicked.setId("clicked");
        NativeButton button = new NativeButton("Click me");
        button.setId("button");
        button.getElement().addEventListener("click",
                event -> clicked.setText(
                        event.getEventData().get(EVENT_DATA).asString()))
                .setFilter(FILTER).addEventData(EVENT_DATA);

        add(status, returned, button, clicked);
    }
}
