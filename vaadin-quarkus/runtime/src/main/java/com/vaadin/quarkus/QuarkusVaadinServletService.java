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

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import io.quarkus.arc.Arc;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationListener;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveListener;
import com.vaadin.flow.router.ListenerPriority;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.ServiceDestroyEvent;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.SessionDestroyEvent;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.SystemMessagesProvider;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.quarkus.annotation.VaadinServiceEnabled;

/**
 * An implementation of {@link com.vaadin.flow.server.VaadinService} for Quarkus
 * environment.
 *
 * This class looks up for and therefore provides an {@link Instantiator} bean.
 * It also forwards Vaadin events to CDI listeners.
 *
 * @author Vaadin Ltd
 */
public class QuarkusVaadinServletService extends VaadinServletService {

    private BeanManager beanManager;

    private final UIEventListener uiEventListener;

    public QuarkusVaadinServletService(final QuarkusVaadinServlet servlet,
            final DeploymentConfiguration configuration,
            final BeanManager beanManager) {
        super(servlet, configuration);
        this.beanManager = beanManager;
        uiEventListener = new UIEventListener(beanManager);
        reportUsage();
    }

    private void reportUsage() {
        if (!getDeploymentConfiguration().isProductionMode()) {
            UsageStatistics.markAsUsed("flow/quarkus", null);
        }
    }

    @Override
    public void init() throws ServiceException {
        addEventListeners();
        lookup(SystemMessagesProvider.class)
                .ifPresent(this::setSystemMessagesProvider);
        super.init();
    }

    @Override
    protected Executor createDefaultExecutor() {
        Instance<Executor> customExecutor = beanManager.createInstance()
                .select(Executor.class, VaadinServiceEnabled.Literal.INSTANCE);
        if (customExecutor.isResolvable()) {
            getLogger().debug("Using custom Vaadin Executor {}",
                    customExecutor.getHandle().getBean());
            return customExecutor.get();
        } else if (customExecutor.isAmbiguous()) {
            String candidates = customExecutor.handlesStream()
                    .map(handle -> handle.getBean().toString())
                    .collect(Collectors.joining(", ", "[", "]"));
            String message = String.format(
                    "Multiple Executor beans annotated with @%1$s found: %2$s. "
                            + "Please make sure a single instance is resolvable.",
                    VaadinServiceEnabled.class.getSimpleName(), candidates);
            throw new IllegalStateException(message);
        }
        Instance<ManagedExecutor> managedExecutors = beanManager
                .createInstance().select(ManagedExecutor.class);
        if (managedExecutors.isResolvable()) {
            getLogger().debug("Using container Managed Executor");
            return managedExecutors.get();
        }
        return super.createDefaultExecutor();
    }

    @Override
    public Optional<Instantiator> loadInstantiators() throws ServiceException {
        final Set<Bean<?>> beans = beanManager.getBeans(
                InstantiatorFactory.class,
                VaadinServiceEnabled.Literal.INSTANCE);
        if (beans == null || beans.isEmpty()) {
            throw new ServiceException("Cannot init VaadinService "
                    + "because no CDI instantiator factory bean found.");
        }
        final Bean<InstantiatorFactory> bean;
        try {
            // noinspection unchecked
            bean = (Bean<InstantiatorFactory>) beanManager.resolve(beans);
        } catch (final AmbiguousResolutionException e) {
            throw new ServiceException("There are multiple eligible CDI "
                    + InstantiatorFactory.class.getSimpleName() + " beans.", e);
        }

        // Return the contextual instance (rather than CDI proxy) as it will be
        // stored inside VaadinService. Not relying on the proxy allows
        // accessing VaadinService::getInstantiator even when
        // VaadinServiceScopedContext is not active
        final CreationalContext<InstantiatorFactory> creationalContext = beanManager
                .createCreationalContext(bean);
        final Context context = beanManager.getContext(ApplicationScoped.class); // VaadinServiceScoped
        final InstantiatorFactory instantiatorFactory = context.get(bean,
                creationalContext);

        Instantiator instantiator = instantiatorFactory.createInstantitor(this);
        if (instantiator == null) {
            throw new ServiceException("Cannot init VaadinService because "
                    + Instantiator.class.getSimpleName() + " is null");
        }
        return Optional.of(instantiator);
    }

    @Override
    public QuarkusVaadinServlet getServlet() {
        return (QuarkusVaadinServlet) super.getServlet();
    }

    private void addEventListeners() {
        addServiceDestroyListener(this::fireCdiDestroyEvent);
        addUIInitListener(uiEventListener);
        addSessionInitListener(this::sessionInit);
        addSessionDestroyListener(this::sessionDestroy);
    }

    private void sessionInit(SessionInitEvent sessionInitEvent)
            throws ServiceException {
        VaadinSession session = sessionInitEvent.getSession();
        lookup(ErrorHandler.class).ifPresent(session::setErrorHandler);
        getBeanManager().getEvent().fire(sessionInitEvent);
    }

    private void sessionDestroy(SessionDestroyEvent sessionDestroyEvent) {
        getBeanManager().getEvent().fire(sessionDestroyEvent);
    }

    private void fireCdiDestroyEvent(ServiceDestroyEvent event) {
        try {
            beanManager.getEvent().fire(event);
        } catch (Exception e) {
            // During application shutdown on TomEE 7,
            // beans are lost at this point.
            // Does not throw an exception, but catch anything just to be sure.
            getLogger().warn("Error at destroy event distribution with CDI.",
                    e);
        }
    }

    /**
     * Gets an instance of a {@code @VaadinServiceEnabled} annotated bean of the
     * given {@code type}.
     *
     * @param type
     *            the required service type
     * @param <T>
     *            the type of the service
     * @return an {@link Optional} wrapping the service instance, or
     *         {@link Optional#empty()} if no bean definition exists for given
     *         type.
     * @throws ServiceException
     *             if multiple beans exists for the given type.
     */
    public <T> Optional<T> lookup(Class<T> type) throws ServiceException {
        try {
            T instance = new BeanLookup<>(getBeanManager(), type,
                    VaadinServiceEnabled.Literal.INSTANCE).lookup();
            return Optional.ofNullable(instance);
        } catch (AmbiguousResolutionException e) {
            throw new ServiceException("There are multiple eligible CDI "
                    + type.getSimpleName() + " beans.", e);
        }
    }

    private BeanManager getBeanManager() {
        return beanManager;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(QuarkusVaadinServletService.class);
    }

    /**
     * Static listener class, to avoid registering the whole service instance.
     */
    @ListenerPriority(-100) // navigation event listeners are last by default
    private static class UIEventListener implements UIInitListener,
            AfterNavigationListener, BeforeEnterListener, BeforeLeaveListener,
            ComponentEventListener<PollEvent> {

        // Not serializable, but can be looked up again from the container
        // after deserialization
        private transient BeanManager beanManager;

        UIEventListener(BeanManager beanManager) {
            this.beanManager = beanManager;
        }

        @Override
        public void uiInit(UIInitEvent event) {
            UI ui = event.getUI();
            ui.addAfterNavigationListener(this);
            ui.addBeforeLeaveListener(this);
            ui.addBeforeEnterListener(this);
            ui.addPollListener(this);
            getBeanManager().getEvent().fire(event);
        }

        @Override
        public void afterNavigation(AfterNavigationEvent event) {
            getBeanManager().getEvent().fire(event);
        }

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            getBeanManager().getEvent().fire(event);
        }

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            getBeanManager().getEvent().fire(event);
        }

        @Override
        public void onComponentEvent(PollEvent event) {
            getBeanManager().getEvent().fire(event);
        }

        private BeanManager getBeanManager() {
            if (beanManager == null) {
                beanManager = Arc.container().beanManager();
            }
            return beanManager;
        }
    }

}
