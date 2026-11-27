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
package com.vaadin.flow.quarkus.it.addons;

import jakarta.annotation.PostConstruct;

import org.vaadin.jandex.HelloWorldJandex;
import org.vaadin.nojandex.HelloWorld;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("addons")
public class AddonsView extends Div {

    public static final String GREET_ID = "greet";
    public static final String WITHOUT_JANDEX_GREETING_ID = "without-jandex-greeting";
    public static final String WITH_JANDEX_GREETING_ID = "with-jandex-greeting";

    @PostConstruct
    void init() {
        HelloWorld helloWorld = new HelloWorld();
        HelloWorldJandex helloWorldJandex = new HelloWorldJandex();
        add(helloWorld, helloWorldJandex);

        Span withoutJandexGreeting = new Span();
        withoutJandexGreeting.setId(WITHOUT_JANDEX_GREETING_ID);
        Span withJandexGreeting = new Span();
        withJandexGreeting.setId(WITH_JANDEX_GREETING_ID);
        NativeButton greet = new NativeButton("Greet from the add-ons",
                event -> {
                    helloWorld.greet("native").then(String.class,
                            withoutJandexGreeting::setText);
                    helloWorldJandex.greet("native").then(String.class,
                            withJandexGreeting::setText);
                });
        greet.setId(GREET_ID);
        add(greet, withoutJandexGreeting, withJandexGreeting);
    }
}
