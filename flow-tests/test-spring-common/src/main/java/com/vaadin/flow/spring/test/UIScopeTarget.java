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
package com.vaadin.flow.spring.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.Route;

@Route("ui-scope")
public class UIScopeTarget extends Div {

    @Component
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public static class InnerComponent extends Div {

        @Autowired
        private UIScopedBean bean;

        @Override
        protected void onAttach(AttachEvent attachEvent) {
            Label label = new Label(String.valueOf(bean.getUid()));
            label.setId("inner");
            add(label);
        }
    }

    public UIScopeTarget(@Autowired UIScopedBean bean,
            @Autowired InnerComponent component) {
        Label label = new Label(String.valueOf(bean.getUid()));
        label.setId("main");
        add(label);

        add(component);
    }

}
