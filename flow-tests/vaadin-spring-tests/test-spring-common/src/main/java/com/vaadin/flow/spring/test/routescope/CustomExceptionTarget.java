/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.spring.test.routescope;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;

public class CustomExceptionTarget extends Div
        implements HasErrorParameter<CustomException> {

    @Autowired
    private ApplicationContext context;

    private boolean isSubDiv;

    private com.vaadin.flow.component.Component current;

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<CustomException> parameter) {
        NativeButton button = new NativeButton("switch content", ev -> {
            remove(current);
            if (isSubDiv) {
                current = context.getBean(CustomExceptionSubButton.class);
            } else {
                current = context.getBean(CustomExceptionSubDiv.class);
            }
            add(current);
            isSubDiv = !isSubDiv;
        });
        button.setId("switch-content");
        add(button);
        current = context.getBean(CustomExceptionSubButton.class);
        add(current);
        return 503;
    }

}
