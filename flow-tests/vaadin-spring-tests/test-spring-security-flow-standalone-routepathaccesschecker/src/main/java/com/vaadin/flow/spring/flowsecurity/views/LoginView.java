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
package com.vaadin.flow.spring.flowsecurity.views;

import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;

@Route("my/login/page")
public class LoginView extends Div {

    public LoginView() {
        setId("login-overlay");

        Form form = new Form();
        form.getElement().setAttribute("action", "my/login/page");
        form.getElement().setAttribute("method", "post");

        form.add(new H2("Spring Security - Flow Test Application"));
        form.add(new Paragraph(
                "Login using john/john (user) or emma/emma (admin)"));

        Input username = new Input();
        username.setId("vaadinLoginUsername");
        username.getElement().setAttribute("name", "username");
        NativeLabel usernameLabel = new NativeLabel("Username");
        usernameLabel.setFor(username);
        form.add(usernameLabel, username);

        Input password = new Input();
        password.setId("vaadinLoginPassword");
        password.setType("password");
        password.getElement().setAttribute("name", "password");
        NativeLabel passwordLabel = new NativeLabel("Password");
        passwordLabel.setFor(password);
        form.add(passwordLabel, password);

        NativeButton submit = new NativeButton("Log in");
        submit.setId("login-submit");
        submit.getElement().setAttribute("type", "submit");
        form.add(submit);

        add(form);
    }

    @Tag("form")
    private static class Form extends HtmlContainer {
    }
}
