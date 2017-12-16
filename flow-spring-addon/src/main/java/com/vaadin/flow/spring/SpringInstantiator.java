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
package com.vaadin.flow.spring;

import java.util.stream.Stream;

import org.springframework.context.ApplicationContext;

import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.common.HasElement;

/**
 * Default Spring instantiator that is used if no other instantiator has been
 * registered. This implementation uses Spring beans.
 *
 * @author Vaadin Ltd
 *
 */
public class SpringInstantiator extends DefaultInstantiator {

    private ApplicationContext context;

    /**
     * Creates a new spring instantiator instance.
     *
     * @param service
     *            the service to use
     * @param context
     *            the application context
     */
    public SpringInstantiator(VaadinService service,
            ApplicationContext context) {
        super(service);
        this.context = context;
    }

    @Override
    public Stream<VaadinServiceInitListener> getServiceInitListeners() {
        Stream<VaadinServiceInitListener> springListeners = context
                .getBeansOfType(VaadinServiceInitListener.class).values()
                .stream();
        return Stream.concat(super.getServiceInitListeners(), springListeners);
    }

    @Override
    public <T extends HasElement> T createRouteTarget(Class<T> routeTargetType,
            NavigationEvent event) {
        return getObject(routeTargetType);
    }

    @Override
    public <T extends Component> T createComponent(Class<T> componentClass) {
        return getObject(componentClass);
    }

    private <T> T getObject(Class<T> type) {
        if (context.getBeanNamesForType(type).length == 1) {
            return context.getBean(type);
        }
        return context.getAutowireCapableBeanFactory().createBean(type);
    }

}
