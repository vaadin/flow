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
package com.vaadin.flow.spring.scopes;

import javax.servlet.ServletContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationListener;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.SpringVaadinSession;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;

/**
 * Implementation of Spring's
 * {@link org.springframework.beans.factory.config.Scope} that binds the beans
 * to a component in the navigation chain. Registered by default as the scope "
 * {@value #VAADIN_ROUTE_SCOPE_NAME}".
 *
 * @see com.vaadin.flow.spring.annotation.VaadinSessionScope
 * @author Vaadin Ltd
 * @since
 *
 */
public class VaadinRouteScope extends AbstractScope implements UIInitListener {

    public static final String VAADIN_ROUTE_SCOPE_NAME = "vaadin-route";

    private static class RouteStoreWrapper implements Serializable {

        private final VaadinSession session;

        private final Registration sessionDestroyListenerRegistration;

        private final Map<String, BeanStore> routeStores;

        private RouteStoreWrapper(VaadinSession session) {
            assert session.hasLock();
            this.session = session;
            routeStores = new HashMap<>();
            if (session instanceof SpringVaadinSession) {
                sessionDestroyListenerRegistration = null;
                ((SpringVaadinSession) session)
                        .addDestroyListener(event -> destroy());
            } else {
                sessionDestroyListenerRegistration = session.getService()
                        .addSessionDestroyListener(event -> destroy());
            }
        }

        private BeanStore getBeanStore(UI ui) {
            assert session.hasLock();
            ExtendedClientDetails details = ui.getInternals()
                    .getExtendedClientDetails();
            String key = getUIStoreKey(ui);
            if (details == null) {
                ui.getPage().retrieveExtendedClientDetails(
                        det -> relocateStore(ui, key));
            }
            BeanStore beanStore = routeStores.get(key);
            if (beanStore == null) {
                beanStore = new RouteBeanStore(ui, session,
                        uiInstance -> routeStores
                                .remove(getUIStoreKey(uiInstance)));
                routeStores.put(key, beanStore);
            }
            return beanStore;
        }

        private void relocateStore(UI ui, String key) {
            assert session.hasLock();
            BeanStore beanStore = routeStores.remove(key);
            if (beanStore == null) {
                LoggerFactory.getLogger(RouteStoreWrapper.class).trace(
                        "UI bean store is not found by the initial UI id via the key '"
                                + key + "'.");
                if (routeStores.get(getUIStoreKey(ui)) == null) {
                    throw new IllegalStateException(
                            "UI bean store is not found by the initial UI id via the key '"
                                    + key + "' and it's not found by the key '"
                                    + getUIStoreKey(ui)
                                    + "' after relocation.");
                }
            } else {
                routeStores.put(getUIStoreKey(ui), beanStore);
            }
        }

        private String getUIStoreKey(UI ui) {
            ExtendedClientDetails details = ui.getInternals()
                    .getExtendedClientDetails();
            if (details == null) {
                return "uid-" + ui.getUIId();
            } else {
                return "win-" + getWindowName(ui);
            }
        }

        private void destroy() {
            session.lock();
            try {
                session.setAttribute(RouteStoreWrapper.class, null);
                routeStores.values().forEach(BeanStore::destroy);
                routeStores.clear();
            } finally {
                session.unlock();
                if (sessionDestroyListenerRegistration != null) {
                    sessionDestroyListenerRegistration.remove();
                }
            }
        }

    }

    private class NavigationListener
            implements BeforeEnterListener, AfterNavigationListener,
            ComponentEventListener<DetachEvent>, Serializable {

        private Class<?> currentNavigationTarget;
        private List<Class<? extends RouterLayout>> currentLayouts;

        private Map<Class<?>, Set<String>> beanNamesByNavigationComponents = new HashMap<>();

        private Registration beforeEnterListener;
        private Registration afterNavigationListener;
        private Registration detachListener;

        private NavigationListener(UI ui) {
            beforeEnterListener = ui.addBeforeEnterListener(this);
            afterNavigationListener = ui.addAfterNavigationListener(this);
            detachListener = ui.addDetachListener(this);
        }

        @Override
        public void afterNavigation(AfterNavigationEvent event) {
            BeanStore store = getBeanStoreIfExists();
            if (store == null) {
                assert beanNamesByNavigationComponents.isEmpty();
            } else {
                Map<Class<?>, Set<String>> activeChain = new HashMap<>();
                event.getActiveChain().stream().map(Object::getClass)
                        .forEach(clazz -> putIfNotNull(activeChain, clazz,
                                beanNamesByNavigationComponents.remove(clazz)));

                Map<Class<?>, Set<String>> notActiveChain = beanNamesByNavigationComponents;
                beanNamesByNavigationComponents = activeChain;

                notActiveChain.values()
                        .forEach(names -> names.forEach(store::remove));
            }
        }

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            currentNavigationTarget = event.getNavigationTarget();
            currentLayouts = event.getLayouts();

            BeanStore store = getBeanStoreIfExists();
            if (store == null) {
                assert beanNamesByNavigationComponents.isEmpty();
            } else {
                Map<Class<?>, Set<String>> activeChain = new HashMap<>();
                putIfNotNull(activeChain, currentNavigationTarget,
                        beanNamesByNavigationComponents
                                .remove(currentNavigationTarget));
                currentLayouts.forEach(layoutClass -> putIfNotNull(activeChain,
                        layoutClass,
                        beanNamesByNavigationComponents.remove(layoutClass)));

                Map<Class<?>, Set<String>> notActiveChain = beanNamesByNavigationComponents;
                beanNamesByNavigationComponents = activeChain;

                notActiveChain.values()
                        .forEach(names -> names.forEach(store::remove));
            }
        }

        @Override
        public void onComponentEvent(DetachEvent event) {
            beforeEnterListener.remove();
            afterNavigationListener.remove();
            detachListener.remove();
        }

        boolean hasNavigationOwner(RouteScopeOwner owner) {
            return owner == null || hasOwnerType(currentNavigationTarget, owner)
                    || layoutsContainsOwner(owner);
        }

        void storeOwner(String name, RouteScopeOwner owner) {
            Class<?> clazz;
            if (owner == null) {
                clazz = currentNavigationTarget;
            } else {
                clazz = owner.value();
            }
            Set<String> set = beanNamesByNavigationComponents
                    .computeIfAbsent(clazz, key -> new HashSet<>());
            set.add(name);
        }

        private boolean hasOwnerType(Class<?> clazz, RouteScopeOwner owner) {
            return clazz != null && clazz.equals(owner.value());
        }

        private boolean layoutsContainsOwner(RouteScopeOwner owner) {
            return currentLayouts != null && currentLayouts.stream()
                    .anyMatch(clazz -> hasOwnerType(clazz, owner));

        }

        private void putIfNotNull(Map<Class<?>, Set<String>> map, Class<?> key,
                Set<String> value) {
            if (value != null) {
                map.put(key, value);
            }
        }
    }

    private static class RouteBeanStore extends BeanStore
            implements ComponentEventListener<DetachEvent> {

        private UI currentUI;

        private Registration uiDetachRegistration;

        private SerializableConsumer<UI> detachUiCallback;

        private RouteBeanStore(UI ui, VaadinSession session,
                SerializableConsumer<UI> detachUiCallback) {
            super(session);
            currentUI = ui;
            uiDetachRegistration = currentUI.addDetachListener(this);
            this.detachUiCallback = detachUiCallback;
        }

        @Override
        public void onComponentEvent(DetachEvent event) {
            assert getVaadinSession().hasLock();
            uiDetachRegistration.remove();
            if (resetUI()) {
                uiDetachRegistration = currentUI.addDetachListener(this);
            } else {
                destroy();
                detachUiCallback.accept(currentUI);
            }
        }

        @Override
        protected Object doGet(String name, ObjectFactory<?> objectFactory) {
            RouteScopeOwner owner = getContext().findAnnotationOnBean(name,
                    RouteScopeOwner.class);
            if (!getNavigationListener().hasNavigationOwner(owner)) {
                throw new IllegalStateException(String.format(
                        "Route owner '%s' instance is not available in the "
                                + "active navigaiton components chain: the scope defined by the bean '%s' doesn't exist.",
                        owner.value(), name));
            }
            return super.doGet(name, objectFactory);
        }

        @Override
        protected void storeBean(String name, Object bean) {
            super.storeBean(name, bean);
            RouteScopeOwner owner = getContext().findAnnotationOnBean(name,
                    RouteScopeOwner.class);
            getNavigationListener().storeOwner(name, owner);
        }

        private boolean resetUI() {
            for (UI ui : getVaadinSession().getUIs()) {
                String windowName = getWindowName(ui);
                if (ui != currentUI && windowName != null
                        && windowName.equals(getWindowName(currentUI))) {
                    currentUI = ui;
                    return true;
                }
            }
            return false;
        }

        private ApplicationContext getContext() {
            VaadinService service = currentUI.getSession().getService();
            VaadinContext context = service.getContext();
            ServletContext servletContext = ((VaadinServletContext) context)
                    .getContext();
            return WebApplicationContextUtils
                    .getWebApplicationContext(servletContext);
        }

        private NavigationListener getNavigationListener() {
            return ComponentUtil.getData(currentUI, NavigationListener.class);
        }

    }

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory) {
        beanFactory.registerScope(VAADIN_ROUTE_SCOPE_NAME, this);
    }

    @Override
    public String getConversationId() {
        return getVaadinSession().getSession().getId() + "-route-scope";
    }

    @Override
    public void uiInit(UIInitEvent event) {
        NavigationListener listener = new NavigationListener(event.getUI());
        ComponentUtil.setData(event.getUI(), NavigationListener.class,
                listener);
    }

    @Override
    protected BeanStore getBeanStore() {
        final VaadinSession session = getVaadinSession();
        session.lock();
        try {
            BeanStore store = getBeanStoreIfExists();
            if (store == null) {
                RouteStoreWrapper wrapper = new RouteStoreWrapper(session);
                session.setAttribute(RouteStoreWrapper.class, wrapper);
                store = wrapper.getBeanStore(getUI());
            }
            return store;
        } finally {
            session.unlock();
        }
    }

    private BeanStore getBeanStoreIfExists() {
        final VaadinSession session = getVaadinSession();
        assert session.hasLock();
        RouteStoreWrapper wrapper = session
                .getAttribute(RouteStoreWrapper.class);
        if (wrapper == null) {
            return null;
        }
        return wrapper.getBeanStore(getUI());
    }

    private static String getWindowName(UI ui) {
        ExtendedClientDetails details = ui.getInternals()
                .getExtendedClientDetails();
        if (details == null) {
            return null;
        }
        return details.getWindowName();
    }

    private static UI getUI() {
        UI ui = UI.getCurrent();
        if (ui == null) {
            throw new IllegalStateException(
                    "There is no UI available. The route scope is not active");
        }
        return ui;
    }

}
