/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.quarkus.it.service;

import jakarta.inject.Inject;

import com.vaadin.flow.quarkus.it.Counter;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.quarkus.annotation.VaadinServiceEnabled;
import com.vaadin.quarkus.annotation.VaadinServiceScoped;

@VaadinServiceEnabled
@VaadinServiceScoped
public class TestErrorHandler implements ErrorHandler {

    @Inject
    private Counter counter;

    @Override
    public void error(ErrorEvent event) {
        counter.increment(TestErrorHandler.class.getSimpleName());
    }
}
