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
package com.vaadin.cdi.context;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.PassivationCapable;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.vaadin.cdi.annotation.RouteScopeOwner;
import com.vaadin.cdi.annotation.RouteScoped;
import com.vaadin.cdi.annotation.VaadinSessionScoped;
import com.vaadin.cdi.util.AbstractContext;
import com.vaadin.cdi.util.BeanProvider;
import com.vaadin.cdi.util.ContextualStorage;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinSession;

import static jakarta.enterprise.event.Reception.IF_EXISTS;

/**
 * Context for {@link RouteScoped @RouteScoped} beans.
 */
public class RouteScopedContext extends AbstractContext {

    @VaadinSessionScoped
    public static class ContextualStorageManager
            extends AbstractContextualStorageManager<RouteStorageKey> {

        /**
         * Prefix of storage identifiers bound to a single UI instance.
         */
        private static final String UI_STORE_ID_PREFIX = "uid-";

        /**
         * Prefix of storage identifiers bound to a browser window, and thus
         * shared by all the UIs of that window.
         */
        private static final String WINDOW_STORE_ID_PREFIX = "win-";

        public ContextualStorageManager() {
            // Session lock checked in VaadinSessionScopedContext while
            // getting the session attribute.
            super(false);
        }

        @Override
        protected ContextualStorage newContextualStorage(RouteStorageKey key) {
            UI.getCurrent().addDetachListener(
                    event -> handleUIDetach(event.getUI(), key));
            return super.newContextualStorage(key);
        }

        private void onAfterNavigation(
                @Observes(notifyObserver = IF_EXISTS) AfterNavigationEvent event) {
            Set<Class<?>> activeChain = event.getActiveChain().stream()
                    .map(Object::getClass).collect(Collectors.toSet());

            destroyDescopedBeans(event.getLocationChangeEvent().getUI(),
                    activeChain);

        }

        private void onBeforeEnter(@Observes BeforeEnterEvent event) {
            UI ui = event.getUI();
            ComponentUtil.setData(ui, NavigationData.class, new NavigationData(
                    event.getNavigationTarget(), event.getLayouts()));

            Set<Class<?>> activeChain = new HashSet<>();
            activeChain.add(event.getNavigationTarget());
            activeChain.addAll(event.getLayouts());

            destroyDescopedBeans(ui, activeChain);
        }

        private void destroyDescopedBeans(UI ui,
                Set<Class<?>> navigationChain) {
            Set<String> uiStoreIds = getUIStoreIds(ui);

            Set<RouteStorageKey> missingKeys = getKeySet().stream()
                    .filter(key -> uiStoreIds.contains(key.getUIId()))
                    .filter(key -> !navigationChain.contains(key.getOwner()))
                    .collect(Collectors.toSet());

            missingKeys.forEach(this::destroy);
        }

        private void handleUIDetach(UI ui, RouteStorageKey key) {
            if (getContextualStorage(key, false) == null) {
                // The storage has been relocated to another key because the
                // scope of its owner changed, or it is already destroyed.
                return;
            }
            if (!key.isWindowScoped()) {
                // The storage belongs to this UI only, so there is nothing to
                // preserve for a potential UI created by a page refresh.
                destroy(key);
                return;
            }
            UI uiAfterRefresh = findPreservingUI(ui);
            if (uiAfterRefresh == null) {
                destroy(key);
            } else {
                uiAfterRefresh.addDetachListener(
                        event -> handleUIDetach(event.getUI(), key));
            }
        }

        private UI findPreservingUI(UI ui) {
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

        private static String getWindowName(UI ui) {
            ExtendedClientDetails details = ui.getInternals()
                    .getExtendedClientDetails();
            return details.getWindowName();
        }

        private RouteStorageKey getKey(UI ui, Class<?> owner) {
            RouteStorageKey uiKey = new RouteStorageKey(owner, getUIStoreId(ui),
                    false);
            String windowName = getWindowName(ui);
            if (windowName == null) {
                return uiKey;
            }
            RouteStorageKey windowKey = new RouteStorageKey(owner,
                    WINDOW_STORE_ID_PREFIX + windowName, true);
            // Beans are shared with the UI created by a page refresh only if
            // the navigation chain is preserved by Flow. In that case the
            // storage is bound to the browser window, exactly like Flow binds
            // the preserved component chain.
            if (isPreserveOnRefreshChain(ui)) {
                return rescope(uiKey, windowKey, ui);
            }
            // The owner may stay in the navigation chain while the chain stops
            // being preserved, for example navigating from a preserved view to
            // a plain sibling of the same layout. Its beans are then bound back
            // to this UI, unless another UI of the same browser window is still
            // alive and may be holding the preserved chain.
            if (getContextualStorage(windowKey, false) != null
                    && findPreservingUI(ui) == null) {
                return rescope(windowKey, uiKey, ui);
            }
            return uiKey;
        }

        /**
         * Moves the storage of an owner whose scope changed, so that its beans
         * are not recreated while the owner stays in the navigation chain.
         *
         * @return the key the storage of the owner is bound to, always
         *         {@code to}
         */
        private RouteStorageKey rescope(RouteStorageKey from,
                RouteStorageKey to, UI ui) {
            if (getContextualStorage(to, false) == null
                    && getContextualStorage(from, false) != null) {
                relocate(from, to);
                // The listener registered for the previous key does not find
                // any storage anymore, so the new key needs its own.
                ui.addDetachListener(
                        event -> handleUIDetach(event.getUI(), to));
            }
            return to;
        }

        private List<ContextualStorage> getActiveContextualStorages() {
            Set<String> uiStoreIds = getUIStoreIds(UI.getCurrent());
            return getKeySet().stream()
                    .filter(key -> uiStoreIds.contains(key.getUIId()))
                    .map(key -> getContextualStorage(key, false))
                    .collect(Collectors.toList());
        }

        /**
         * Gets all the storage identifiers a UI can hold beans for: its own
         * one, plus the one shared by all the UIs of the same browser window. A
         * single UI may own both kinds of storage, for example after navigating
         * from a regular view to a {@link PreserveOnRefresh} one.
         */
        private Set<String> getUIStoreIds(UI ui) {
            Set<String> ids = new HashSet<>();
            ids.add(getUIStoreId(ui));
            String windowName = getWindowName(ui);
            if (windowName != null) {
                ids.add(WINDOW_STORE_ID_PREFIX + windowName);
            }
            return ids;
        }

        private String getUIStoreId(UI ui) {
            return UI_STORE_ID_PREFIX + ui.getUIId();
        }

        private static boolean isPreserveOnRefreshChain(UI ui) {
            NavigationData data = ComponentUtil.getData(ui,
                    NavigationData.class);
            if (data == null) {
                return false;
            }
            return isPreserveOnRefresh(data.getNavigationTarget())
                    || data.getLayouts().stream().anyMatch(
                            ContextualStorageManager::isPreserveOnRefresh);
        }

        private static boolean isPreserveOnRefresh(Class<?> clazz) {
            return clazz != null
                    && clazz.isAnnotationPresent(PreserveOnRefresh.class);
        }

    }

    private static class RouteStorageKey implements Serializable {
        private final Class<?> owner;
        private final String uiId;
        // Derived from uiId, so it is not part of equals/hashCode
        private final boolean windowScoped;

        private RouteStorageKey(Class<?> owner, String uiId,
                boolean windowScoped) {
            this.owner = owner;
            this.uiId = uiId;
            this.windowScoped = windowScoped;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof RouteStorageKey)) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            RouteStorageKey key = (RouteStorageKey) obj;
            return owner.equals(key.owner) && uiId.equals(key.uiId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(owner, uiId);
        }

        @Override
        public String toString() {
            return "[ ui-key='" + getUIId() + "', owner='" + getOwner() + "' ]";
        }

        Class<?> getOwner() {
            return owner;
        }

        String getUIId() {
            return uiId;
        }

        /**
         * Whether the storage is shared by all the UIs of the same browser
         * window, as required to preserve beans of a {@link PreserveOnRefresh}
         * navigation chain.
         */
        boolean isWindowScoped() {
            return windowScoped;
        }

    }

    static class NavigationData implements Serializable {
        private final Class<?> navigationTarget;
        private final List<Class<? extends RouterLayout>> layouts;

        NavigationData(Class<?> navigationTarget,
                List<Class<? extends RouterLayout>> layouts) {
            this.navigationTarget = navigationTarget;
            this.layouts = layouts;
        }

        Class<?> getNavigationTarget() {
            return navigationTarget;
        }

        List<Class<? extends RouterLayout>> getLayouts() {
            return layouts;
        }
    }

    private ContextualStorageManager contextManager;
    private Supplier<Boolean> isUIContextActive;
    private BeanManager beanManager;

    public RouteScopedContext(BeanManager beanManager) {
        super(beanManager);
    }

    public void init(BeanManager beanManager,
            Supplier<Boolean> isUIContextActive) {
        contextManager = BeanProvider.getContextualReference(beanManager,
                ContextualStorageManager.class, false);
        this.beanManager = beanManager;
        this.isUIContextActive = isUIContextActive;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return RouteScoped.class;
    }

    @Override
    public boolean isActive() {
        return isUIContextActive.get();
    }

    @Override
    protected List<ContextualStorage> getActiveContextualStorages() {
        return contextManager.getActiveContextualStorages();
    }

    @Override
    protected ContextualStorage getContextualStorage(Contextual<?> contextual,
            boolean createIfNotExist) {
        Bean<?> bean = getBean(contextual);
        UI ui = UI.getCurrent();
        Class<?> owner = getOwner(ui, bean);
        if (!navigationChainHasOwner(ui, owner) && createIfNotExist) {
            throw new IllegalStateException(String.format(
                    "Route owner '%s' instance is not available in the "
                            + "active navigation components chain: the scope defined by the bean '%s' doesn't exist.",
                    owner, bean.getBeanClass().getName()));
        }
        RouteStorageKey key = contextManager.getKey(ui, owner);
        return contextManager.getContextualStorage(key, createIfNotExist);
    }

    private boolean navigationChainHasOwner(UI ui, Class<?> owner) {
        NavigationData data = ComponentUtil.getData(ui, NavigationData.class);
        if (owner.equals(data.getNavigationTarget())) {
            return true;
        }
        return data.getLayouts().stream()
                .anyMatch(clazz -> clazz.equals(owner));
    }

    @SuppressWarnings("unchecked")
    private Class<?> getOwner(UI ui, Bean<?> bean) {
        return bean.getQualifiers().stream()
                .filter(annotation -> annotation instanceof RouteScopeOwner)
                .map(annotation -> (Class<?>) (((RouteScopeOwner) annotation)
                        .value()))
                .findFirst()
                .orElseGet(() -> getCurrentNavigationTarget(ui, bean));
    }

    @SuppressWarnings("rawtypes")
    private Class getCurrentNavigationTarget(UI ui, Bean<?> bean) {
        NavigationData data = ComponentUtil.getData(ui, NavigationData.class);
        if (data == null) {
            throw new IllegalStateException(String.format(
                    "There is no yet any navigation chain available, "
                            + "so bean '%s' has no scope and may not be injected",
                    bean.getBeanClass().getName()));
        }
        if (data.getLayouts().contains(bean.getBeanClass()))
            return bean.getBeanClass();
        return data.getNavigationTarget();
    }

    private Bean<?> getBean(Contextual<?> contextual) {
        if (contextual instanceof Bean) {
            return (Bean<?>) contextual;
        }
        if (contextual instanceof PassivationCapable) {
            String id = ((PassivationCapable) contextual).getId();
            return beanManager.getPassivationCapableBean(id);
        } else {
            throw new IllegalArgumentException(contextual.getClass().getName()
                    + " is not of type " + Bean.class.getName());
        }
    }
}
