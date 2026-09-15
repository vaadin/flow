/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.quarkus;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import jakarta.enterprise.inject.spi.BeanManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.router.PageTitleGenerator;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.auth.MenuAccessControl;
import com.vaadin.quarkus.annotation.VaadinServiceEnabled;

/**
 * Instantiator implementation for Quarkus.
 *
 * New instances are created by default by QuarkusInstantiatorFactory.
 *
 * @see InstantiatorFactory
 */
public class QuarkusInstantiator implements Instantiator {

    private static final String CANNOT_USE_CDI_BEANS_FOR_I18N = "Cannot use CDI beans for I18N, falling back to the default behavior.";
    private static final String FALLING_BACK_TO_DEFAULT_INSTANTIATION = "Falling back to default instantiation.";

    private AtomicBoolean i18NLoggingEnabled = new AtomicBoolean(true);
    private DefaultInstantiator delegate;

    private BeanManager beanManager;

    public QuarkusInstantiator(DefaultInstantiator delegate,
            BeanManager beanManager) {
        this.delegate = delegate;
        this.beanManager = beanManager;
    }

    /**
     * Gets the {@link BeanManager} instance.
     *
     * @return the {@link BeanManager} instance.
     */
    public BeanManager getBeanManager() {
        return this.beanManager;
    }

    @Override
    public <T> T getOrCreate(Class<T> type) {
        return new BeanLookup<>(getBeanManager(), type)
                .setUnsatisfiedHandler(() -> getLogger().debug(
                        "'{}' is not a CDI bean. "
                                + FALLING_BACK_TO_DEFAULT_INSTANTIATION,
                        type.getName()))
                .setAmbiguousHandler(
                        e -> getLogger().debug(
                                "Multiple CDI beans found. "
                                        + FALLING_BACK_TO_DEFAULT_INSTANTIATION,
                                e))
                .lookupOrElseGet(() -> {
                    final T instance = delegate.getOrCreate(type);
                    // BeanProvider.injectFields(instance); // TODO maybe it
                    // could be fixed after Quarkus-Arc ticket
                    // https://github.com/quarkusio/quarkus/issues/2378 is done
                    return instance;
                });
    }

    @Override
    public I18NProvider getI18NProvider() {
        final BeanLookup<I18NProvider> lookup = new BeanLookup<>(
                getBeanManager(), I18NProvider.class,
                VaadinServiceEnabled.Literal.INSTANCE);
        if (i18NLoggingEnabled.compareAndSet(true, false)) {
            lookup.setUnsatisfiedHandler(() -> getLogger().info(
                    "Can't find any @VaadinServiceScoped bean implementing '{}'. "
                            + CANNOT_USE_CDI_BEANS_FOR_I18N,
                    I18NProvider.class.getSimpleName())).setAmbiguousHandler(
                            e -> getLogger().warn(
                                    "Found more beans for I18N. "
                                            + CANNOT_USE_CDI_BEANS_FOR_I18N,
                                    e));
        } else {
            lookup.setAmbiguousHandler(e -> {
            });
        }
        return lookup.lookupOrElseGet(delegate::getI18NProvider);
    }

    @Override
    public MenuAccessControl getMenuAccessControl() {
        final BeanLookup<MenuAccessControl> lookup = new BeanLookup<>(
                getBeanManager(), MenuAccessControl.class,
                VaadinServiceEnabled.Literal.INSTANCE);
        return lookup.lookupOrElseGet(delegate::getMenuAccessControl);
    }

    @Override
    public PageTitleGenerator getPageTitleGenerator() {
        final BeanLookup<PageTitleGenerator> lookup = new BeanLookup<>(
                getBeanManager(), PageTitleGenerator.class,
                VaadinServiceEnabled.Literal.INSTANCE);
        return lookup.lookupOrElseGet(delegate::getPageTitleGenerator);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(QuarkusInstantiator.class);
    }

    @Override
    public Stream<VaadinServiceInitListener> getServiceInitListeners() {

        final BeanLookup<VaadinServiceInitListener> lookup = new BeanLookup<>(
                getBeanManager(), VaadinServiceInitListener.class,
                VaadinServiceEnabled.Literal.INSTANCE);
        return Stream.concat(
                Stream.concat(delegate.getServiceInitListeners(),
                        lookup.lookupAll()),
                Stream.of(event -> getBeanManager().getEvent().fire(event)));
    }

    @Override
    public <T extends Component> T createComponent(
            final Class<T> componentClass) {
        return getOrCreate(componentClass);
    }
}
