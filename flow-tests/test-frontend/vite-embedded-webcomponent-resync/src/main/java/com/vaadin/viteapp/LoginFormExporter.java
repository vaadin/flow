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
package com.vaadin.viteapp;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponent;

public class LoginFormExporter extends WebComponentExporter<LoginForm> {
    public LoginFormExporter() {
        super("login-form");
        addProperty("userlbl", "").onChange(LoginForm::setUserNameLabel);
        addProperty("pwdlbl", "").onChange(LoginForm::setPasswordLabel);
    }

    @Override
    protected void configureInstance(WebComponent<LoginForm> webComponent,
            LoginForm form) {
    }
}
