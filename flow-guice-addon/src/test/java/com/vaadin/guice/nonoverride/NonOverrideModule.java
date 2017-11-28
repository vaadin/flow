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
package com.vaadin.guice.nonoverride;

import com.google.inject.AbstractModule;

import com.vaadin.guice.testClasses.AnImplementation;
import com.vaadin.guice.testClasses.AnInterface;
import com.vaadin.guice.testClasses.AnotherInterface;
import com.vaadin.guice.testClasses.AnotherInterfaceImplementation;

public class NonOverrideModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(AnInterface.class).to(AnImplementation.class);
        bind(AnotherInterface.class).to(AnotherInterfaceImplementation.class);
    }
}
