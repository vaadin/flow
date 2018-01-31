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

import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

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
    public I18NProvider getI18NProvider() {
        int beansCount = context.getBeanNamesForType(I18NProvider.class).length;
        if (beansCount == 1) {
            return context.getBean(I18NProvider.class);
        } else {
            LoggerFactory.getLogger(SpringInstantiator.class.getName()).info(
                    "The number of beans implementing '{}' is {}. Cannot use Spring beans for I18N, "
                            + "falling back to the default behavior",
                    I18NProvider.class.getSimpleName(), beansCount);
            return super.getI18NProvider();
        }
    }

    @Override
    public <T> T getOrCreate(Class<T> type) {
        if (context.getBeanNamesForType(type).length == 1) {
            return context.getBean(type);
        }

        return context.getAutowireCapableBeanFactory().createBean(type);
    }
}
