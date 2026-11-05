/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import com.vaadin.flow.router.internal.RouteUtil;
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
 * @since 18.0
 */
public class VaadinRouteScope extends AbstractScope {

    /** Name the scope is registered under. */
    public static final String VAADIN_ROUTE_SCOPE_NAME = "vaadin-route";

    private static class RouteStoreWrapper implements Serializable {

        /** Prefix of the keys of stores bound to a single UI. */
        private static final String UI_STORE_KEY_PREFIX = "uid-";

        /**
         * Prefix of the keys of stores bound to a browser window, and thus
         * shared by all the UIs of that window.
         */
        private static final String WINDOW_STORE_KEY_PREFIX = "win-";

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
            if (getWindowName(ui) == null) {
                // A preserved navigation chain can only be bound to the browser
                // window once its name is known, so that a UI created by a page
                // refresh finds the beans.
                ui.getPage().retrieveExtendedClientDetails(
                        details -> resolveBeanStore(ui));
            }
            RouteBeanStore beanStore = resolveBeanStore(ui);
            if (beanStore == null) {
                String key = getStoreKey(ui);
                beanStore = new RouteBeanStore(ui, session, key,
                        store -> routeStores.remove(store.key, store));
                routeStores.put(key, beanStore);
            }
            if (!ui.equals(beanStore.currentUI)) {
                // Reloading for new UI on same window name. Update UI for
                // beanStore.
                beanStore.currentUI = ui;
            }
            return beanStore;
        }

        /**
         * Resolves the existing store of the given UI. The store is moved
         * between the UI and the browser window keys when the navigation chain
         * of the UI starts or stops being preserved on refresh, so that its
         * beans are not recreated while their owners stay in the chain.
         */
        private RouteBeanStore resolveBeanStore(UI ui) {
            assert session.hasLock();
            String uiKey = getUIStoreKey(ui);
            String windowName = getWindowName(ui);
            if (windowName == null) {
                return routeStores.get(uiKey);
            }
            String windowKey = WINDOW_STORE_KEY_PREFIX + windowName;
            if (isPreserveOnRefreshChain(ui)) {
                return rescope(ui, uiKey, windowKey);
            }
            // The window store is bound back to this UI only if this UI is the
            // one that used it last. Otherwise it belongs to another UI of the
            // same browser window, e.g. one left open by a crashed tab.
            RouteBeanStore windowStore = routeStores.get(windowKey);
            if (windowStore != null && ui.equals(windowStore.currentUI)) {
                return rescope(ui, windowKey, uiKey);
            }
            return routeStores.get(uiKey);
        }

        private RouteBeanStore rescope(UI ui, String fromKey, String toKey) {
            RouteBeanStore fromStore = routeStores.remove(fromKey);
            RouteBeanStore toStore = routeStores.get(toKey);
            if (fromStore == null) {
                return toStore;
            }
            if (toStore != null) {
                // The beans of the other store are the ones the navigation
                // chain uses, so the beans this UI held under the previous key
                // are out of scope.
                fromStore.uiDetachRegistration.remove();
                fromStore.destroy();
                return toStore;
            }
            fromStore.key = toKey;
            // The store may have been listening to an earlier UI of the same
            // browser window, but it now belongs to this UI only.
            fromStore.uiDetachRegistration.remove();
            fromStore.uiDetachRegistration = ui.addDetachListener(fromStore);
            routeStores.put(toKey, fromStore);
            return fromStore;
        }

        private String getStoreKey(UI ui) {
            String windowName = getWindowName(ui);
            if (windowName != null && isPreserveOnRefreshChain(ui)) {
                return WINDOW_STORE_KEY_PREFIX + windowName;
            }
            return getUIStoreKey(ui);
        }

        private static boolean isPreserveOnRefreshChain(UI ui) {
            NavigationListener listener = ComponentUtil.getData(ui,
                    NavigationListener.class);
            return listener != null && listener.currentNavigationTarget != null
                    && RouteUtil.isPreserveOnRefreshTarget(
                            listener.currentNavigationTarget,
                            listener.currentLayouts);
        }

        private static String getUIStoreKey(UI ui) {
            return UI_STORE_KEY_PREFIX + ui.getUIId();
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

        private final Registration beforeEnterListener;
        private final Registration afterNavigationListener;
        private final Registration detachListener;

        private final UI ui;

        private NavigationListener(UI ui) {
            beforeEnterListener = ui.addBeforeEnterListener(this);
            afterNavigationListener = ui.addAfterNavigationListener(this);
            detachListener = ui.addDetachListener(this);
            this.ui = ui;
        }

        @Override
        public void afterNavigation(AfterNavigationEvent event) {
            BeanStore store = getBeanStoreIfExists(ui.getSessionOrThrow());
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

            BeanStore store = getBeanStoreIfExists(ui.getSessionOrThrow());
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
            RouteBeanStore beanStore = getBeanStoreIfExists(
                    ui.getSessionOrThrow());
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

        private String key;

        private Registration uiDetachRegistration;

        private final SerializableConsumer<RouteBeanStore> detachUiCallback;

        private final BeanNamesWrapper beanNames = new BeanNamesWrapper();

        private RouteBeanStore(UI ui, VaadinSession session, String key,
                SerializableConsumer<RouteBeanStore> detachUiCallback) {
            super(session);
            currentUI = ui;
            this.key = key;
            uiDetachRegistration = currentUI.addDetachListener(this);
            this.detachUiCallback = detachUiCallback;
        }

        @Override
        public void onComponentEvent(DetachEvent event) {
            assert getVaadinSession().hasLock();
            uiDetachRegistration.remove();
            // Only a store bound to the browser window outlives its UI, to be
            // taken over by the UI that a page refresh creates.
            if (isWindowScoped() && resetUI(event.getUI())) {
                uiDetachRegistration = currentUI.addDetachListener(this);
            } else {
                destroy();
                detachUiCallback.accept(this);
            }
        }

        private boolean isWindowScoped() {
            return key.startsWith(RouteStoreWrapper.WINDOW_STORE_KEY_PREFIX);
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

        /**
         * Re-points this store to the UI that replaces the given detached UI on
         * the same browser window, if any.
         * <p>
         * The detached UI is passed in explicitly because {@code currentUI} may
         * already have been re-assigned to a newer UI by
         * {@link RouteStoreWrapper#getBeanStore(UI)}, in which case it is not
         * the UI this detach event originates from.
         *
         * @param detachedUI
         *            the UI being detached
         * @return {@code true} if a replacement UI was found
         */
        private boolean resetUI(UI detachedUI) {
            UI ui = findPreservingUI(detachedUI);
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

    /**
     * Creates the scope. Spring instantiates it.
     */
    public VaadinRouteScope() {
        // Default constructor
    }

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
        return details.getWindowName();
    }

    private static UI getUI() {
        return UI.getCurrentOrThrow();
    }

    /**
     * Finds the UI that took over the browser window of the given UI, e.g. on a
     * page refresh.
     *
     * @param ui
     *            the UI being detached
     * @return the UI on the same browser window, or {@code null} if there is
     *         none
     */
    private static UI findPreservingUI(UI ui) {
        VaadinSession session = ui.getSession();
        String windowName = getWindowName(ui);
        if (session == null || windowName == null) {
            // Without a window name there is nothing to match the session UIs
            // against, and a UI that has already been detached from the session
            // cannot be matched at all.
            return null;
        }
        for (UI sessionUi : session.getUIs()) {
            if (sessionUi != ui
                    && windowName.equals(getWindowName(sessionUi))) {
                return sessionUi;
            }
        }
        return null;
    }

}
