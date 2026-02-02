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
package com.vaadin.flow.uitest.ui.vitelogout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;

/**
 * Login view for testing Vite logout redirect behavior.
 * <p>
 * Contains a native HTML form that posts to the login route, which is
 * intercepted by {@link MockAuthenticationFilter}.
 */
@Route("com.vaadin.flow.uitest.ui.vitelogout.LoginView")
public class LoginView extends Div {

    public LoginView() {
        Element form = new Element("form");
        form.setAttribute("action",
                "/view/com.vaadin.flow.uitest.ui.vitelogout.LoginView");
        form.setAttribute("method", "POST");

        NativeButton submit = new NativeButton("Login");
        submit.setId("login-button");
        submit.getElement().setAttribute("type", "submit");

        getElement().appendChild(form);
        form.appendChild(submit.getElement());
    }
}
