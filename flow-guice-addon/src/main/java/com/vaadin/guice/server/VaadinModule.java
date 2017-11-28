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
package com.vaadin.guice.server;

import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.ProvisionListener;
import com.vaadin.guice.annotation.Controller;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.guice.annotation.VaadinSessionScope;

class VaadinModule extends AbstractModule {

    private final GuiceVaadinServlet guiceVaadinServlet;

    VaadinModule(GuiceVaadinServlet GuiceVaadinServlet) {
        this.guiceVaadinServlet = GuiceVaadinServlet;
    }

    @Override
    protected void configure() {
        bindScope(UIScope.class, guiceVaadinServlet.getUiScope());
        bindScope(VaadinSessionScope.class, guiceVaadinServlet.getVaadinSessionScoper());

        for (Class<?> controllerClass : guiceVaadinServlet.getControllerClasses()) {
            final Controller annotation = controllerClass.getAnnotation(Controller.class);

            bindListener(
                    new AbstractMatcher<Binding<?>>() {
                        @Override
                        public boolean matches(Binding<?> binding) {

                            final Class<?> rawType = binding.getKey().getTypeLiteral().getRawType();

                            return annotation.value().equals(rawType);
                        }
                    },
                    new ProvisionListener() {
                        @Override
                        public <T> void onProvision(ProvisionInvocation<T> provisionInvocation) {
                            provisionInvocation.provision();

                            guiceVaadinServlet.getInjector().getInstance(controllerClass);
                        }
                    }
            );
        }
    }
}
