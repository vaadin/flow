/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.SpringVersion;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

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
    }

    @Override
    public Stream<VaadinServiceInitListener> getServiceInitListeners() {
        Stream<VaadinServiceInitListener> springListeners = context
                .getBeansOfType(VaadinServiceInitListener.class).values()
                .stream();
        return Stream.concat(super.getServiceInitListeners(), springListeners);
    }

    @Override
    public <T extends Component> T createComponent(Class<T> componentClass) {
        return context.getAutowireCapableBeanFactory()
                .createBean(componentClass);
    }

    @Override
    public I18NProvider getI18NProvider() {
        int beansCount = context.getBeanNamesForType(I18NProvider.class).length;
        if (beansCount == 1) {
            return context.getBean(I18NProvider.class);
        } else {
            if (loggingEnabled.compareAndSet(true, false)) {
                LoggerFactory.getLogger(SpringInstantiator.class.getName())
                        .info("The number of beans implementing '{}' is {}. Cannot use Spring beans for I18N, "
                                + "falling back to the default behavior",
                                I18NProvider.class.getSimpleName(), beansCount);
            }
            return super.getI18NProvider();
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
        if (context.getBeanNamesForType(type).length == 1) {
            return context.getBean(type);
        } else if (context.getBeanNamesForType(type).length > 1) {
            try {
                return context.getAutowireCapableBeanFactory().createBean(type);
            } catch (BeanInstantiationException e) {
                throw new BeanInstantiationException(e.getBeanClass(),
                        "[HINT] This could be caused by more than one suitable beans for autowiring in the context.",
                        e);
            }
        } else {
            // If there is no bean, try to instantiate one
            return context.getAutowireCapableBeanFactory().createBean(type);
        }
    }
}
