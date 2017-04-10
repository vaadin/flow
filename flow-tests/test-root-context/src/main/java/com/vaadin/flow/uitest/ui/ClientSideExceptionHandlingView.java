/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import com.vaadin.flow.html.Button;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.router.View;

public class ClientSideExceptionHandlingView extends Div implements View {

    static final String CAUSE_EXCEPTION_ID = "causeException";
    private Button causeException;

    public ClientSideExceptionHandlingView() {
        causeException = new Button("Cause client side exception", e -> {
            getUI().get().getPage().executeJavaScript("null.foo");
        });
        causeException.setId(CAUSE_EXCEPTION_ID);
        add(causeException);
    }
}
