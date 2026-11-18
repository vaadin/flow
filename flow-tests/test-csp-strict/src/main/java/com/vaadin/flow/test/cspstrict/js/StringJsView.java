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

import java.util.Arrays;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.CspViolationException;

/**
 * Calls {@code executeJs(String)}, which the strict mode rejects in Java at the
 * call: the exception, and the frame of this view in its stack trace, are shown
 * in the page instead of a browser violation nobody can trace.
 */
@Route("string-js")
public class StringJsView extends Div {

    public StringJsView() {
        Span message = new Span();
        message.setId("message");
        Span callSite = new Span();
        callSite.setId("call-site");
        add(message, callSite);
        try {
            getElement().executeJs("this.textContent = $0", "never runs");
            message.setText("no exception");
        } catch (CspViolationException e) {
            message.setText(e.getMessage());
            callSite.setText(Arrays.stream(e.getStackTrace())
                    .filter(frame -> frame.getClassName()
                            .equals(StringJsView.class.getName()))
                    .findFirst().map(StackTraceElement::toString)
                    .orElse("no frame of this view in the trace"));
        }
    }
}
