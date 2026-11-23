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
package com.example.application;

import jakarta.annotation.PostConstruct;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLayout;

/**
 * A layout is created as a bean from the definition the AOT processing
 * registers for it, so Spring calls its lifecycle callbacks.
 */
public class MainLayout extends Div implements RouterLayout {

    public static final String INITIALIZED_ID = "layout-initialized";

    @PostConstruct
    void init() {
        Span initialized = new Span("Layout initialized");
        initialized.setId(INITIALIZED_ID);
        add(initialized);
    }
}
