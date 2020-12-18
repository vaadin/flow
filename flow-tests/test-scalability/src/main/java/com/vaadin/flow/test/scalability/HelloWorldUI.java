package com.vaadin.flow.test.scalability;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServlet;

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

public class HelloWorldUI extends UI {

    public static final String PATH = "/helloworld/";

    public static final String HELLO_WORLD_UI = HelloWorldUI.class.getName();

    /**
     * The main servlet for the application.
     */
    @WebServlet(urlPatterns = PATH
            + "*", name = "UIServlet", asyncSupported = true, initParams = {
                    @WebInitParam(name = "ui", value = "com.vaadin.flow.test.scalability.HelloWorldUI"),
                    @WebInitParam(name = "productionMode", value = "false") })
    public static class Servlet extends VaadinServlet {
    }

    @Override
    protected void init(VaadinRequest request) {
        NativeButton b = new NativeButton("Hello", e -> {
            add(new Text("Hello!"));
        });
        add(b);
    }
}
