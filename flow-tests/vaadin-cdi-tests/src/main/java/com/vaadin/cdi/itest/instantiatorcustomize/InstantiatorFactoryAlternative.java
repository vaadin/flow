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
package com.vaadin.cdi.itest.instantiatorcustomize;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.interceptor.Interceptor;

import com.vaadin.cdi.annotation.VaadinServiceEnabled;
import com.vaadin.cdi.annotation.VaadinServiceScoped;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.server.VaadinService;

@Priority(Interceptor.Priority.APPLICATION)
@Alternative
@VaadinServiceEnabled
@VaadinServiceScoped
public class InstantiatorFactoryAlternative implements InstantiatorFactory {

    @Override
    public Instantiator createInstantitor(VaadinService service) {
        return new InstantiatorAlternative();
    }
}
