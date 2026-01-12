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
package com.vaadin.flow.spring.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequestInterceptor;
import com.vaadin.flow.server.VaadinServiceInitListener;

@Component
public class MyVaadinServiceInitListener implements VaadinServiceInitListener {

    private final List<VaadinRequestInterceptor> interceptors;

    public MyVaadinServiceInitListener(
            @Autowired List<VaadinRequestInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        interceptors.forEach(event::addVaadinRequestInterceptor);
    }
}
