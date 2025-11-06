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
package com.vaadin.flow.spring.scopes;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

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
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;

/**
 * Implementation of Spring's
 * {@link org.springframework.beans.factory.config.Scope} that binds the beans
 * to a component in the navigation chain. Registered by default as the scope "
 * {@value #VAADIN_ROUTE_SCOPE_NAME}".
 *
 * @see com.vaadin.flow.spring.annotation.VaadinSessionScope
 * @author Vaadin Ltd
 *
 */
public class VaadinRouteScope extends AbstractScope {

    public static final String VAADIN_ROUTE_SCOPE_NAME = "vaadin-route";

    private static class RouteStoreWrapper implements Serializable {

        private final VaadinSession session;

        private final Map<String, RouteBeanStore> routeStores;

        private RouteStoreWrapper(VaadinSession session) {
            assert session.hasLock();
            this.session = session;
            session.addSessionDestroyListener(event -> destroy());
            routeStores = new HashMap<>();
        }

        private RouteBeanStore getBeanStore(UI ui) {
            assert session.hasLock();
            ExtendedClientDetails details = ui.getInternals()
                    .getExtendedClientDetails();
            String key = getUIStoreKey(ui);
            if (details == null) {
                ui.getPage().retrieveExtendedClientDetails(
                        det -> relocateStore(ui, key));
            }
            RouteBeanStore beanStore = routeStores.get(key);
            if (beanStore == null) {
                beanStore = new RouteBeanStore(ui, session,
                        uiInstance -> routeStores
                                .remove(getUIStoreKey(uiInstance)));
                routeStores.put(key, beanStore);
            }
            if (!ui.equals(beanStore.currentUI)) {
                // Reloading for new UI on same window name. Update UI for
                // beanStore.
                beanStore.currentUI = ui;
            }
            return beanStore;
        }

        private void relocateStore(UI ui, String key) {
            assert session.hasLock();
            RouteBeanStore beanStore = routeStores.remove(key);
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
            }
        }

    }

    private static class NavigationListener
            implements BeforeEnterListener, AfterNavigationListener,
            ComponentEventListener<DetachEvent>, Serializable {

        private Class<?> currentNavigationTarget;
        private List<Class<? extends RouterLayout>> currentLayouts;

        private Registration beforeEnterListener;
        private Registration afterNavigationListener;
        private Registration detachListener;

        private final UI ui;

        private NavigationListener(UI ui) {
            beforeEnterListener = ui.addBeforeEnterListener(this);
            afterNavigationListener = ui.addAfterNavigationListener(this);
            detachListener = ui.addDetachListener(this);
            this.ui = ui;
        }

        @Override
        public void afterNavigation(AfterNavigationEvent event) {
            BeanStore store = getBeanStoreIfExists(ui.getSession());
            if (store == null) {
                assert getBeanNamesByNavigationComponents().isEmpty();
            } else {
                Map<Class<?>, Set<String>> activeChain = new HashMap<>();
                event.getActiveChain().stream().map(Object::getClass)
                        .forEach(clazz -> putIfNotNull(activeChain, clazz,
                                removeBeansByNavigationComponent(clazz)));

                Map<Class<?>, Set<String>> notActiveChain = getBeanNamesByNavigationComponents();
                setBeanNamesByNavigationComponents(activeChain);

                notActiveChain.values()
                        .forEach(names -> names.forEach(store::remove));
            }
        }

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            currentNavigationTarget = event.getNavigationTarget();
            currentLayouts = event.getLayouts();

            BeanStore store = getBeanStoreIfExists(ui.getSession());
            if (store == null) {
                assert getBeanNamesByNavigationComponents().isEmpty();
            } else {
                Map<Class<?>, Set<String>> activeChain = new HashMap<>();
                putIfNotNull(activeChain, currentNavigationTarget,
                        removeBeansByNavigationComponent(
                                currentNavigationTarget));
                currentLayouts.forEach(
                        layoutClass -> putIfNotNull(activeChain, layoutClass,
                                removeBeansByNavigationComponent(layoutClass)));

                Map<Class<?>, Set<String>> notActiveChain = getBeanNamesByNavigationComponents();
                setBeanNamesByNavigationComponents(activeChain);

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
            Set<String> set = getBeanNamesByNavigationComponents()
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

        private BeanNamesWrapper getBeanNamesWrapper() {
            RouteBeanStore beanStore = getBeanStoreIfExists(ui.getSession());
            return beanStore == null ? null : beanStore.getBeanNamesWrapper();
        }

        private Map<Class<?>, Set<String>> getBeanNamesByNavigationComponents() {
            BeanNamesWrapper wrapper = getBeanNamesWrapper();
            if (wrapper == null) {
                return Collections.emptyMap();
            }
            return wrapper.beanNamesByNavigationComponents;
        }

        private Set<String> removeBeansByNavigationComponent(Class<?> clazz) {
            Map<Class<?>, Set<String>> map = getBeanNamesByNavigationComponents();
            if (map.isEmpty()) {
                return null;
            } else {
                return map.remove(clazz);
            }
        }

        private void setBeanNamesByNavigationComponents(
                Map<Class<?>, Set<String>> map) {
            BeanNamesWrapper wrapper = getBeanNamesWrapper();
            if (wrapper != null) {
                wrapper.beanNamesByNavigationComponents = map;
            }
        }
    }

    private static class BeanNamesWrapper implements Serializable {

        private Map<Class<?>, Set<String>> beanNamesByNavigationComponents = new HashMap<>();
    }

    private static class RouteBeanStore extends BeanStore
            implements ComponentEventListener<DetachEvent> {

        private UI currentUI;

        private Registration uiDetachRegistration;

        private SerializableConsumer<UI> detachUiCallback;

        private final BeanNamesWrapper beanNames = new BeanNamesWrapper();

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
            RouteScopeObjectFactory cast = (RouteScopeObjectFactory) objectFactory;
            RouteScopeOwner owner = cast.getOwner();
            if (!getNavigationListener().hasNavigationOwner(owner)) {
                assert owner != null;
                throw new IllegalStateException(String.format(
                        "Route owner '%s' instance is not available in the "
                                + "active navigation components chain: the scope defined by the bean '%s' doesn't exist.",
                        owner.value(), name));
            }
            Object object = super.doGet(name, objectFactory);
            if (object instanceof ObjectWithOwner wrapper) {
                return wrapper.object;
            }
            return object;
        }

        @Override
        protected void storeBean(String name, Object bean) {
            ObjectWithOwner wrapper = (ObjectWithOwner) bean;
            super.storeBean(name, wrapper.object);
            getNavigationListener().storeOwner(name, wrapper.owner);
        }

        BeanNamesWrapper getBeanNamesWrapper() {
            return beanNames;
        }

        private boolean resetUI() {
            UI ui = findPreservingUI(currentUI);
            if (ui == null) {
                return false;
            }
            currentUI = ui;
            return true;
        }

        @NonNull
        private NavigationListener getNavigationListener() {
            NavigationListener navigationListener = ComponentUtil
                    .getData(currentUI, NavigationListener.class);
            assert navigationListener != null;
            return navigationListener;
        }

    }

    private record ObjectWithOwner(Object object, RouteScopeOwner owner) {
    }

    private static class RouteScopeObjectFactory
            implements ObjectFactory<ObjectWithOwner> {

        private final ObjectFactory<?> objectFactory;
        private final RouteScopeOwner owner;

        public RouteScopeObjectFactory(ObjectFactory<?> objectFactory,
                RouteScopeOwner owner) {
            this.objectFactory = objectFactory;
            this.owner = owner;
        }

        @Override
        public ObjectWithOwner getObject() throws BeansException {
            return new ObjectWithOwner(objectFactory.getObject(), owner);
        }

        public RouteScopeOwner getOwner() {
            return owner;
        }
    }

    static class NavigationListenerRegistrar implements UIInitListener {

        @Override
        public void uiInit(UIInitEvent event) {
            NavigationListener listener = new NavigationListener(event.getUI());
            ComponentUtil.setData(event.getUI(), NavigationListener.class,
                    listener);
        }

    }

    private ConfigurableListableBeanFactory beanFactory;

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory) {
        beanFactory.registerScope(VAADIN_ROUTE_SCOPE_NAME, this);
        beanFactory.registerSingleton(
                NavigationListenerRegistrar.class.getName(),
                new NavigationListenerRegistrar());
        this.beanFactory = beanFactory;
    }

    @Override
    public String getConversationId() {
        return getVaadinSession().getSession().getId() + "-route-scope";
    }

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        return super.get(name, new RouteScopeObjectFactory(objectFactory,
                beanFactory.findAnnotationOnBean(name, RouteScopeOwner.class)));
    }

    @Override
    protected BeanStore getBeanStore() {
        final VaadinSession session = getVaadinSession();
        session.getLockInstance().lock();
        try {
            BeanStore store = getBeanStoreIfExists(session);
            if (store == null) {
                RouteStoreWrapper wrapper = new RouteStoreWrapper(session);
                session.setAttribute(RouteStoreWrapper.class, wrapper);
                store = wrapper.getBeanStore(getUI());
            }
            return store;
        } finally {
            session.getLockInstance().unlock();
        }
    }

    private static RouteBeanStore getBeanStoreIfExists(VaadinSession session) {
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
        return UI.getCurrentOrThrow();
    }

    private static UI findPreservingUI(UI ui) {
        VaadinSession session = ui.getSession();
        String windowName = getWindowName(ui);
        for (UI sessionUi : session.getUIs()) {
            if (sessionUi != ui && windowName != null
                    && windowName.equals(getWindowName(sessionUi))) {
                return sessionUi;
            }
        }
        return null;
    }

}
