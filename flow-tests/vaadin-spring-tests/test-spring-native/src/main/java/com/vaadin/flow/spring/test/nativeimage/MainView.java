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
package com.vaadin.flow.spring.test.nativeimage;

import org.springframework.stereotype.Service;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("")
public class MainView extends Div {

    public static final String BINDER_LINK_ID = "binder-link";
    public static final String GREETING_ID = "greeting";

    /**
     * Created by Spring and injected into the view, which is itself created as
     * a bean from the definition the AOT processing registers for it.
     */
    @Service
    public static class GreetingService {

        public String getGreeting() {
            return "Hello from a Spring bean";
        }
    }

    public MainView(GreetingService greetingService) {
        Span greeting = new Span(greetingService.getGreeting());
        greeting.setId(GREETING_ID);
        RouterLink binderLink = new RouterLink("Binder", BinderView.class);
        binderLink.setId(BINDER_LINK_ID);
        add(greeting, binderLink);
    }
}
