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
package com.vaadin.flow.spring;

import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.SpringBootVersion;
import org.springframework.context.ApplicationContext;
import org.springframework.core.SpringVersion;
import org.springframework.util.ClassUtils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.auth.MenuAccessControl;

/**
 * Default Spring instantiator that is used if no other instantiator has been
 * registered. This implementation uses Spring beans.
 *
 * @author Vaadin Ltd
 */
public class SpringInstantiator extends DefaultInstantiator {

    private ApplicationContext context;
    private AtomicBoolean loggingEnabled = new AtomicBoolean(true);

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

        UsageStatistics.markAsUsed("flow/SpringInstantiator", null);
        UsageStatistics.markAsUsed("SpringFramework", Optional
                .ofNullable(SpringVersion.getVersion()).orElse("unknown"));
        if (SpringUtil.isSpringBoot()) {
            UsageStatistics.markAsUsed("SpringBoot",
                    SpringBootVersion.getVersion());
        }
    }

    /**
     * Gets all service init listeners to use. Registers by default an init
     * listener that publishes ServiceInitEvent to application context.
     *
     * @return stream of service init listeners, not <code>null</code>
     */
    @Override
    public Stream<VaadinServiceInitListener> getServiceInitListeners() {
        Stream<VaadinServiceInitListener> springListeners = Stream
                .concat(Stream.of(event -> {
                    // make ServiceInitEvent listenable with @EventListener
                    context.publishEvent(event);
                }), context.getBeansOfType(VaadinServiceInitListener.class)
                        .values().stream());
        return Stream.concat(super.getServiceInitListeners(), springListeners);
    }

    @Override
    public <T extends Component> T createComponent(Class<T> componentClass) {
        return context.getAutowireCapableBeanFactory()
                .createBean(componentClass);
    }

    @Override
    public I18NProvider getI18NProvider() {
        try {
            return context.getBean(I18NProvider.class);
        } catch (NoSuchBeanDefinitionException ignored) {
            if (loggingEnabled.compareAndSet(true, false)) {
                LoggerFactory.getLogger(SpringInstantiator.class.getName())
                        .info("The number of beans implementing '{}' is {} why spring could not provide a unique bean. "
                                + "Cannot use Spring beans for I18N, falling back to the default behavior",
                                I18NProvider.class.getSimpleName(),
                                context.getBeanNamesForType(
                                        I18NProvider.class).length);
            }
            return super.getI18NProvider();
        }
    }

    @Override
    public MenuAccessControl getMenuAccessControl() {
        try {
            return context.getBean(MenuAccessControl.class);
        } catch (NoSuchBeanDefinitionException ignored) {
            if (loggingEnabled.compareAndSet(true, false)) {
                LoggerFactory.getLogger(SpringInstantiator.class.getName())
                        .info("The number of beans implementing '{}' is {} why spring could not provide a unique bean. "
                                + "Cannot use Spring beans for Menu Access Control, falling back to the default behavior",
                                MenuAccessControl.class.getSimpleName(),
                                context.getBeanNamesForType(
                                        MenuAccessControl.class).length);
            }
            return super.getMenuAccessControl();
        }
    }

    /**
     * Hands over an existing bean or tries to instantiate one with the
     * following rules:
     * <ul>
     * <li>If exactly one bean is present in the context, it returns this bean.
     * </li>
     * <li>If no bean is present, it tries to instantiate one.</li>
     * <li>If more than one bean is present, it tries to instantiate one but in
     * case of a Bean instantiation exception this exception is catched and
     * rethrown with a hint. Reason for this is, that users may expect it to
     * "use" a bean but have multiple in the context. So the hint helps them
     * find the problem.</li>
     * </ul>
     */
    @Override
    public <T> T getOrCreate(Class<T> type) {
        try {
            return context.getBean(type);
        } catch (NoUniqueBeanDefinitionException e) {
            // there are multiple beans of this type in the context, but spring
            // couldn't figure out which one to use
            // to stay compatible with previous implementation we try to create
            // a new bean
            // this will always fail if type is an interface or abstract though
            // so we won't try but give a meaningful message instead
            if (type.isInterface()
                    || Modifier.isAbstract(type.getModifiers())) {
                throw new BeanInstantiationException(type,
                        "[HINT] There is multiple Beans of this type and spring could not resolve the ambiguity, but also the given type is an interface or abstract so we cannot try to create a new bean of this type.",
                        e);
            }
            try {
                return context.getAutowireCapableBeanFactory().createBean(type);
            } catch (BeanInstantiationException ie) {
                throw new BeanInstantiationException(ie.getBeanClass(),
                        "[HINT] This was caused because there are Multiple beans of this type, spring could not resolve the ambiguity and creating a new bean of this type also failed.",
                        ie);
            }
        } catch (NoSuchBeanDefinitionException ignored) {
            // If there is no bean, try to instantiate one
            return context.getAutowireCapableBeanFactory().createBean(type);
        }
    }

    @Override
    public Class<?> getApplicationClass(Class<?> clazz) {
        return ClassUtils.getUserClass(clazz);
    }
}
