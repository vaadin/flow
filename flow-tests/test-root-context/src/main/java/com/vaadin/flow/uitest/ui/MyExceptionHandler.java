/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.uitest.MyException;

/**
 * The exception handler for the
 *
 * @since 1.0
 */
public class MyExceptionHandler extends Div
        implements HasErrorParameter<MyException> {

    public MyExceptionHandler() {
        Label label = new Label("My exception handler.");
        label.setId("custom-exception");
        add(label);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<MyException> parameter) {
        return 404;
    }
}
