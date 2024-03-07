/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.LookupInitializer;
import com.vaadin.flow.function.VaadinApplicationInitializationBootstrap;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServletContext;

/**
 * Spring aware {@link LookupInitializer} implementation.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
public class SpringLookupInitializer extends LookupInitializer {

    private static final Object LOCK = new Object();

    private static interface BootstrapCallable {

        void execute() throws ServletException;
    }

    private static class ApplicationContextWrapper {
        private WebApplicationContext appContext;
    }

    private static class SpringLookup extends LookupImpl {

        private final WebApplicationContext context;

        private SpringLookup(WebApplicationContext context,
                BiFunction<Class<?>, Class<?>, Object> factory,
                Map<Class<?>, Collection<Class<?>>> services) {
            super(services, factory);
            this.context = context;
        }

        @Override
        public <T> T lookup(Class<T> serviceClass) {
            Collection<T> beans = context.getBeansOfType(serviceClass).values();

            // Check whether we have service objects instantiated without Spring
            T service = super.lookup(serviceClass);

            Collection<T> allFound;
            if (service == null || (beans.size() > 0 && service.getClass()
                    .getPackage().getName().startsWith("com.vaadin.flow"))) {
                // Ignore service impl class (from the super lookup) if it's
                // absent or it's a default implementation and there are Spring
                // beans
                allFound = beans;
            } else {
                allFound = new ArrayList<>(beans.size() + 1);
                allFound.addAll(beans);
                allFound.add(service);
            }
            if (allFound.size() == 0) {
                return null;
            } else if (allFound.size() == 1) {
                return allFound.iterator().next();
            }
            throw new IllegalStateException(SEVERAL_IMPLS + serviceClass + SPI
                    + allFound + ONE_IMPL_REQUIRED);
        }

        @Override
        public <T> Collection<T> lookupAll(Class<T> serviceClass) {
            return Stream
                    .concat(context.getBeansOfType(serviceClass).values()
                            .stream(), super.lookupAll(serviceClass).stream())
                    .collect(Collectors.toList());
        }

    }

    static class SpringApplicationContextInit
            implements ApplicationContextAware {

        @Override
        public void setApplicationContext(ApplicationContext applicationContext)
                throws BeansException {
            BootstrapCallable callable = null;
            if (applicationContext instanceof WebApplicationContext) {
                synchronized (LOCK) {
                    ServletContext servletContext = ((WebApplicationContext) applicationContext)
                            .getServletContext();
                    VaadinServletContext vaadinServletContext = new VaadinServletContext(
                            servletContext);
                    callable = vaadinServletContext
                            .getAttribute(BootstrapCallable.class);
                    vaadinServletContext
                            .removeAttribute(BootstrapCallable.class);
                    ApplicationContextWrapper wrapper = new ApplicationContextWrapper();
                    wrapper.appContext = (WebApplicationContext) applicationContext;
                    vaadinServletContext.setAttribute(wrapper);
                }
            }
            if (callable != null) {
                try {
                    callable.execute();
                } catch (ServletException exception) {
                    throw new IllegalStateException(exception);
                }
            }
        }

    }

    @Override
    public void initialize(VaadinContext context,
            Map<Class<?>, Collection<Class<?>>> services,
            VaadinApplicationInitializationBootstrap bootstrap)
            throws ServletException {
        VaadinServletContext servletContext = (VaadinServletContext) context;
        boolean isContextAvailable = false;
        synchronized (LOCK) {
            ApplicationContext appContext = getApplicationContext(context);
            isContextAvailable = appContext != null;
            if (!isContextAvailable) {
                context.setAttribute(BootstrapCallable.class,
                        () -> doInitialize(servletContext, services,
                                bootstrap));
            }
        }
        if (isContextAvailable) {
            doInitialize(servletContext, services, bootstrap);
        }
    }

    @Override
    protected Lookup createLookup(VaadinContext context,
            Map<Class<?>, Collection<Class<?>>> services) {
        WebApplicationContext appContext = getApplicationContext(context);
        return new SpringLookup(appContext,
                (spi, impl) -> instantiate(appContext, spi, impl), services);
    }

    private void doInitialize(VaadinContext context,
            Map<Class<?>, Collection<Class<?>>> services,
            VaadinApplicationInitializationBootstrap bootstrap)
            throws ServletException {
        super.initialize(context, services, bootstrap);
    }

    /**
     * Gets a {@link WebApplicationContext} instance for the {@code context}.
     *
     * @param context
     *            a Vaadin context
     * @return a {@link WebApplicationContext} instance for the {@code context}
     */
    static WebApplicationContext getApplicationContext(VaadinContext context) {
        VaadinServletContext servletContext = (VaadinServletContext) context;
        WebApplicationContext appContext = WebApplicationContextUtils
                .getWebApplicationContext(servletContext.getContext());
        if (appContext == null) {
            // Spring behavior is always unbelievably surprising: under some
            // circumstances {@code appContext} may be null even though the app
            // context has been set via ApplicationContextAware: no idea WHY
            ApplicationContextWrapper wrapper = context
                    .getAttribute(ApplicationContextWrapper.class);
            appContext = wrapper == null ? null : wrapper.appContext;
        }
        return appContext;
    }

    private <T> T instantiate(WebApplicationContext context,
            Class<T> serviceClass, Class<?> impl) {
        Collection<T> beans = context.getBeansOfType(serviceClass).values();
        if (beans.stream().anyMatch(bean -> impl.isInstance(bean))) {
            // implementation classes found in classpath are ignored if there
            // are beans which are subclasses of these impl classes
            return null;
        } else {
            return instantiate(serviceClass, impl);
        }
    }

}
