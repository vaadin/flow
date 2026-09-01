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
package com.vaadin.cdi;

import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.cdi.annotation.VaadinServiceEnabled;
import com.vaadin.cdi.annotation.VaadinServiceScoped;
import com.vaadin.cdi.context.VaadinSessionScopedContext;
import com.vaadin.cdi.util.BeanManagerProvider;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.function.DeploymentConfiguration;
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
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;

import static com.vaadin.cdi.BeanLookup.SERVICE;

/**
 * Servlet service implementation for Vaadin CDI.
 * <p>
 * This class creates and initializes a @{@link VaadinServiceEnabled}
 * {@link Instantiator}.
 * <p>
 * Some @{@link VaadinServiceEnabled} beans can be used to customize Vaadin,
 * they are also created, and bound if found.
 * <ul>
 * <li>{@link SystemMessagesProvider} is bound to service by
 * {@link VaadinService#setSystemMessagesProvider(SystemMessagesProvider)}.
 * <li>{@link ErrorHandler} is bound to created sessions by
 * {@link VaadinSession#setErrorHandler(ErrorHandler)}.
 * </ul>
 *
 * @see CdiVaadinServlet
 */
public class CdiVaadinServletService extends VaadinServletService {

    private final CdiVaadinServiceDelegate delegate;

    public CdiVaadinServletService(CdiVaadinServlet servlet,
            DeploymentConfiguration configuration, BeanManager beanManager) {
        super(servlet, configuration);
        this.delegate = new CdiVaadinServiceDelegate(beanManager);
    }

    @Override
    public void init() throws ServiceException {
        delegate.init(this);
        super.init();
    }

    @Override
    protected VaadinSession loadSession(WrappedSession wrappedSession) {
        return super.loadSession(wrappedSession);
    }

    @Override
    protected void storeSession(VaadinSession session,
            WrappedSession wrappedSession) {
        super.storeSession(session, wrappedSession);
    }

    private void restoreDelegate(VaadinSession session) {

    }

    @Override
    protected Executor createDefaultExecutor() {
        Instance<VaadinTaskExecutorSelector> instance = delegate
                .getBeanManager().createInstance()
                .select(VaadinTaskExecutorSelector.class);
        VaadinTaskExecutorSelector selector = instance.get();
        Executor executor = selector.getExecutor()
                .orElseGet(super::createDefaultExecutor);
        instance.destroy(selector);
        return executor;
    }

    public Optional<Instantiator> loadInstantiators() throws ServiceException {
        BeanManager beanManager = delegate.getBeanManager();

        final Set<Bean<?>> beans = beanManager
                .getBeans(InstantiatorFactory.class, SERVICE);
        if (beans == null || beans.isEmpty()) {
            throw new ServiceException("Cannot init VaadinService "
                    + "because no CDI instantiator factory bean found.");
        }
        final Bean<InstantiatorFactory> bean;
        try {
            // noinspection unchecked
            bean = (Bean<InstantiatorFactory>) beanManager.resolve(beans);
        } catch (AmbiguousResolutionException e) {
            throw new ServiceException("There are multiple eligible CDI "
                    + InstantiatorFactory.class.getSimpleName() + " beans.", e);
        }

        // Return the contextual instance (rather than CDI proxy) as it will be
        // stored inside VaadinService. Not relying on the proxy allows
        // accessing VaadinService::getInstantiator even when
        // VaadinServiceScopedContext is not active
        final CreationalContext<InstantiatorFactory> creationalContext = beanManager
                .createCreationalContext(bean);
        final Context context = beanManager
                .getContext(VaadinServiceScoped.class);
        final InstantiatorFactory instantiatorFactory = context.get(bean,
                creationalContext);
        Instantiator instantiator = instantiatorFactory.createInstantitor(this);

        if (instantiator == null) {
            throw new ServiceException("Cannot init VaadinService because "
                    + " no Instantiator is available. Please check your "
                    + " InstatiatorFactory CDI bean implementation.");
        }
        return Optional.of(instantiator);
    }

    public CdiVaadinServlet getServlet() {
        return (CdiVaadinServlet) super.getServlet();
    }

    /**
     * This class implements the actual instantiation and event brokering
     * functionality of {@link CdiVaadinServletService}.
     */
    public static class CdiVaadinServiceDelegate implements Serializable {

        private transient BeanManager beanManager;

        private final UIEventListener uiEventListener;

        public CdiVaadinServiceDelegate(BeanManager beanManager) {
            this.beanManager = beanManager;
            uiEventListener = new UIEventListener(this);
        }

        public void init(VaadinService vaadinService) throws ServiceException {
            lookup(SystemMessagesProvider.class)
                    .ifPresent(vaadinService::setSystemMessagesProvider);
            vaadinService.addUIInitListener(uiEventListener);
            vaadinService.addSessionInitListener(this::sessionInit);
            vaadinService.addSessionDestroyListener(this::sessionDestroy);
            vaadinService.addServiceDestroyListener(this::fireCdiDestroyEvent);
        }

        public void addUIListeners(UI ui) {
            ui.addAfterNavigationListener(uiEventListener);
            ui.addBeforeLeaveListener(uiEventListener);
            ui.addBeforeEnterListener(uiEventListener);
            ui.addPollListener(uiEventListener);
            ui.addDetachListener(e -> getBeanManager().getEvent()
                    .fire(new UIDetachEvent(e)));
        }

        public <T> Optional<T> lookup(Class<T> type) throws ServiceException {
            try {
                T instance = new BeanLookup<>(getBeanManager(), type, SERVICE)
                        .lookup();
                return Optional.ofNullable(instance);
            } catch (AmbiguousResolutionException e) {
                throw new ServiceException("There are multiple eligible CDI "
                        + type.getSimpleName() + " beans.", e);
            }
        }

        public BeanManager getBeanManager() {
            if (beanManager == null) {
                beanManager = BeanManagerProvider.getInstance()
                        .getBeanManager();
            }
            return beanManager;
        }

        private void sessionInit(SessionInitEvent sessionInitEvent)
                throws ServiceException {
            VaadinSession session = sessionInitEvent.getSession();
            lookup(ErrorHandler.class).ifPresent(session::setErrorHandler);
            getBeanManager().getEvent().fire(sessionInitEvent);
        }

        private void sessionDestroy(SessionDestroyEvent sessionDestroyEvent) {
            getBeanManager().getEvent().fire(sessionDestroyEvent);
            if (VaadinSessionScopedContext.guessContextIsUndeployed()) {
                // Happens on tomcat when it expires sessions upon undeploy.
                // beanManager.getPassivationCapableBean returns null for
                // passivation id,
                // so we would get an NPE from AbstractContext.destroyAllActive
                getLogger().warn("VaadinSessionScoped context does not exist. "
                        + "Maybe application is undeployed."
                        + " Can't destroy VaadinSessionScopedContext.");
                return;
            }
            getLogger().debug("VaadinSessionScopedContext destroy");
            VaadinSessionScopedContext
                    .destroy(sessionDestroyEvent.getSession());
        }

        private void fireCdiDestroyEvent(ServiceDestroyEvent event) {
            try {
                getBeanManager().getEvent().fire(event);
            } catch (Exception e) {
                // During application shutdown on TomEE 7,
                // beans are lost at this point.
                // Does not throw an exception, but catch anything just to be
                // sure.
                getLogger().warn(
                        "Error at destroy event distribution with CDI.", e);
            }
        }

        private static Logger getLogger() {
            return LoggerFactory.getLogger(CdiVaadinServiceDelegate.class);
        }
    }

    /**
     * Static listener class, to avoid registering the whole service instance.
     */
    @ListenerPriority(-100) // navigation event listeners are last by default
    private static class UIEventListener implements UIInitListener,
            AfterNavigationListener, BeforeEnterListener, BeforeLeaveListener,
            ComponentEventListener<PollEvent> {

        private final CdiVaadinServiceDelegate delegate;

        private UIEventListener(CdiVaadinServiceDelegate delegate) {
            this.delegate = delegate;
        }

        @Override
        public void uiInit(UIInitEvent event) {
            // UI listeners must be attached before observers are notified of
            // the UI initialization
            delegate.addUIListeners(event.getUI());
            delegate.getBeanManager().getEvent().fire(event);
        }

        @Override
        public void afterNavigation(AfterNavigationEvent event) {
            delegate.getBeanManager().getEvent().fire(event);
        }

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            delegate.getBeanManager().getEvent().fire(event);
        }

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            delegate.getBeanManager().getEvent().fire(event);
        }

        @Override
        public void onComponentEvent(PollEvent event) {
            delegate.getBeanManager().getEvent().fire(event);
        }
    }
}
