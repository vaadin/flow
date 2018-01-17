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
package com.vaadin.flow.tutorial.spring;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("spring/tutorial-spring-scopes.asciidoc")
public class Scopes {

    public static class SessionScoped {
        @Route("")
        public class MainLayout extends Div {
            public MainLayout(@Autowired SessionService bean) {
                setText(bean.getText());
            }

            public void edit() {
                getUI().get().navigateTo("editor");
            }
        }

        @Route("editor")
        public class Editor extends Div {
            public Editor(@Autowired SessionService bean) {
                setText(bean.getText());
            }
        }
    }

    public static class UIScoped {
        @Route("")
        public class MainLayout extends Div {
            public MainLayout(@Autowired UIService bean) {
                setText(bean.getText());
            }

            public void edit() {
                getUI().get().navigateTo("editor");
            }
        }

        @Route("editor")
        public class Editor extends Div {
            public Editor(@Autowired UIService bean) {
                setText(bean.getText());
            }
        }
    }

    @Component
    @VaadinSessionScope
    public class SessionService {
        private String uid = UUID.randomUUID().toString();

        public String getText() {
            return "session " + uid;
        }
    }

    @Component
    @UIScope
    public class UIService {
        private String uid = UUID.randomUUID().toString();

        public String getText() {
            return "ui " + uid;
        }
    }

}
