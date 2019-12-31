/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ClientSideExceptionHandlingView", layout = ViewTestLayout.class)
@JavaScript("externalErrorTrigger.js")
public class ClientSideExceptionHandlingView extends Div {

    static final String CAUSE_EXCEPTION_ID = "causeException";
    private NativeButton causeException;

    public ClientSideExceptionHandlingView() {
        causeException = new NativeButton("Cause client side exception", e -> {
            getUI().get().getPage().executeJs("null.foo");
        });
        causeException.setId(CAUSE_EXCEPTION_ID);

        /*
         * Used for manually testing that the name of an offending external
         * function is actually reported in the browser.
         */
        NativeButton causeExternalException = new NativeButton(
                "Cause external client side exception", e -> {
                    getUI().get().getPage()
                            .executeJs("externalErrorTrigger()");
                });

        add(causeException, causeExternalException);
    }
}
